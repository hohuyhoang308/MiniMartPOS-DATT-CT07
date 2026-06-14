package com.pos.config;

import com.pos.entity.*;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.entity.enums.PaymentMethod;
import com.pos.entity.enums.ShiftStatus;
import com.pos.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seed dữ liệu demo PHONG PHÚ cho đồ án: ~130 mặt hàng tiện lợi (kèm thuế GTGT + quy cách đóng gói),
 * 4 nhà cung cấp, nhiều phiếu nhập theo lô (HSD đa dạng — vài món cận hạn để minh hoạ cảnh báo),
 * lên kệ ~60%, và <b>lịch sử bán hàng 30 ngày</b> để Dashboard / Báo cáo / ABC-XYZ có số liệu thật.
 *
 * <p>Idempotent: sản phẩm theo mã vạch (8930+số thứ tự); chỉ seed phần còn thiếu. Lịch sử bán chỉ tạo
 * khi CHƯA có hóa đơn nào. Chỉ chạy ở profile KHÁC prod.</p>
 */
@Component
@Profile("!prod")
@Order(20)
public class CatalogDemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDemoDataInitializer.class);

    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final UserRepository userRepository;
    private final ShelfTransferRepository shelfTransferRepository;
    private final ShelfRepository shelfRepository;
    private final WorkShiftRepository workShiftRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;

    @PersistenceContext
    private EntityManager em;

    public CatalogDemoDataInitializer(CategoryRepository categoryRepository, UnitRepository unitRepository,
                                      SupplierRepository supplierRepository, ProductRepository productRepository,
                                      GoodsReceiptRepository goodsReceiptRepository, UserRepository userRepository,
                                      ShelfTransferRepository shelfTransferRepository, ShelfRepository shelfRepository,
                                      WorkShiftRepository workShiftRepository, InvoiceRepository invoiceRepository,
                                      CustomerRepository customerRepository, StoreRepository storeRepository) {
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.userRepository = userRepository;
        this.shelfTransferRepository = shelfTransferRepository;
        this.shelfRepository = shelfRepository;
        this.workShiftRepository = workShiftRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.storeRepository = storeRepository;
    }

    /** Đặc tả 1 mặt hàng demo (thuế + quy cách suy ra từ danh mục/đơn vị). */
    private record Spec(String name, String category, String unit,
                        long cost, long sale, int minStock, int stockQty, int shelfLifeDays) {}

    @Override
    @Transactional
    public void run(String... args) {
        Store ch01 = storeRepository.findByCode("CH01").orElse(null);
        Store ch02 = storeRepository.findByCode("CH02").orElse(null);
        if (ch01 == null) {
            log.warn("Bỏ qua seed catalog: chưa có chi nhánh CH01.");
            return;
        }
        Map<String, Category> categories = ensureCategories();
        Map<String, Unit> units = ensureUnits();
        List<Supplier> suppliers = ensureSuppliers();

        // 1) SẢN PHẨM cấp CHUỖI (dùng chung mọi chi nhánh) — tạo nếu thiếu (idempotent theo mã vạch).
        List<Spec> specs = specs();
        Map<Spec, Product> productBySpec = ensureProducts(specs, categories, units);

        // 2) TỒN KHO + KỆ + LỊCH SỬ BÁN theo TỪNG CHI NHÁNH (mỗi chi nhánh có kho/kệ/ca/hóa đơn riêng).
        //    Random seed khác nhau để dữ liệu 2 chi nhánh không trùng khít.
        seedStoreInventory(ch01, "manager", "staff", suppliers, specs, productBySpec, 42L);
        if (ch02 != null) {
            seedStoreInventory(ch02, "manager2", "staff2", suppliers, specs, productBySpec, 43L);
        }
    }

    /**
     * Tạo (nếu thiếu) toàn bộ sản phẩm cấp CHUỖI từ {@link #specs()} và trả về map Spec→Product để
     * các chi nhánh dùng chung khi seed tồn kho. Idempotent theo mã vạch {@code 8930+số thứ tự}.
     */
    private Map<Spec, Product> ensureProducts(List<Spec> specs, Map<String, Category> categories,
                                              Map<String, Unit> units) {
        Map<Spec, Product> map = new LinkedHashMap<>();
        int created = 0, idx = 0;
        for (Spec s : specs) {
            String barcode = String.format("8930%09d", ++idx);
            Product p = productRepository.findByBarcode(barcode).orElse(null);
            if (p == null) {
                p = new Product();
                p.setBarcode(barcode);
                p.setName(s.name());
                p.setCategory(categories.get(s.category()));
                p.setUnit(units.get(s.unit()));
                p.setCostPrice(BigDecimal.valueOf(s.cost()));
                p.setSalePrice(BigDecimal.valueOf(s.sale()));
                p.setTaxRate(BigDecimal.valueOf(taxOf(s.category())));
                int[] pack = packOf(s.category(), s.unit());
                p.setPackSize(pack[0]);
                if (pack[0] > 1) p.setPackUnit(units.get(pack[1] == 24 ? "Thùng" : "Lốc"));
                p.setMinStock(s.minStock());
                p.setStatus(CommonStatus.ACTIVE);
                p = productRepository.save(p);
                created++;
            }
            map.put(s, p);
        }
        if (created > 0) log.info("Đã seed {} sản phẩm (cấp chuỗi).", created);
        return map;
    }

    /**
     * Seed TỒN KHO ban đầu + lên kệ + lịch sử bán 12 tuần cho MỘT chi nhánh. Idempotent theo chi nhánh:
     * nếu chi nhánh đã có phiếu nhập thì coi như đã seed và bỏ qua (không đụng dữ liệu đang có).
     *
     * @param store           chi nhánh đích
     * @param managerUsername tài khoản người lập phiếu (fallback admin)
     * @param cashierUsername tài khoản thu ngân cho lịch sử bán (fallback = người lập phiếu)
     * @param randomSeed      seed ngẫu nhiên (khác nhau giữa các chi nhánh để dữ liệu không trùng)
     */
    private void seedStoreInventory(Store store, String managerUsername, String cashierUsername,
                                    List<Supplier> suppliers, List<Spec> specs,
                                    Map<Spec, Product> productBySpec, long randomSeed) {
        if (!goodsReceiptRepository.findByStoreIdOrderByCreatedAtDesc(store.getId()).isEmpty()) {
            return; // chi nhánh đã có tồn kho → đã seed trước đó
        }
        User createdBy = userRepository.findByUsername(managerUsername)
                .or(() -> userRepository.findByUsername("admin")).orElse(null);
        if (createdBy == null) {
            log.warn("Bỏ qua seed tồn kho {}: chưa có tài khoản {}/admin.", store.getCode(), managerUsername);
            return;
        }
        User cashier = userRepository.findByUsername(cashierUsername).orElse(createdBy);
        Map<String, Shelf> shelves = ensureShelves(store);

        // batch theo từng NCC để tạo nhiều phiếu nhập (chỉ những mặt hàng có tồn đầu kỳ > 0)
        List<List<GoodsReceiptItem>> batchesBySupplier = new ArrayList<>();
        List<List<String>> catsBySupplier = new ArrayList<>();
        for (int i = 0; i < suppliers.size(); i++) { batchesBySupplier.add(new ArrayList<>()); catsBySupplier.add(new ArrayList<>()); }

        for (Spec s : specs) {
            if (s.stockQty() <= 0) continue;
            Product p = productBySpec.get(s);
            if (p == null) continue;
            GoodsReceiptItem item = new GoodsReceiptItem();
            item.setProduct(p);
            item.setQuantity(s.stockQty());
            item.setImportPrice(BigDecimal.valueOf(s.cost()));
            item.setExpiryDate(s.shelfLifeDays() > 0 ? LocalDate.now().plusDays(s.shelfLifeDays()) : null);
            int sup = supplierOf(s.category());
            batchesBySupplier.get(sup).add(item);
            catsBySupplier.get(sup).add(s.category());
        }

        // Mỗi NCC một phiếu nhập; gom lô + đưa lên kệ tới khi gần đầy (tôn trọng SỨC CHỨA kệ).
        Map<Long, int[]> shelfLoad = new HashMap<>();                 // shelfId -> [đang chứa]
        Map<Long, Product> productsWithStock = new LinkedHashMap<>(); // productId -> Product (để sinh lịch sử bán)
        for (int sup = 0; sup < suppliers.size(); sup++) {
            List<GoodsReceiptItem> batches = batchesBySupplier.get(sup);
            if (batches.isEmpty()) continue;
            GoodsReceipt receipt = new GoodsReceipt();
            receipt.setStore(store);
            receipt.setCode(nextReceiptCode());
            receipt.setSupplier(suppliers.get(sup));
            receipt.setCreatedBy(createdBy);
            receipt.setNote("Nhập tồn kho ban đầu (demo) - " + suppliers.get(sup).getName());
            BigDecimal total = BigDecimal.ZERO;
            for (GoodsReceiptItem it : batches) {
                receipt.addItem(it);
                total = total.add(it.getImportPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            }
            receipt.setTotalAmount(total);
            goodsReceiptRepository.save(receipt); // cascade -> lô có id

            for (int i = 0; i < batches.size(); i++) {
                GoodsReceiptItem it = batches.get(i);
                productsWithStock.put(it.getProduct().getId(), it.getProduct());
                Shelf shelf = shelves.get(catsBySupplier.get(sup).get(i));
                if (shelf == null) continue;
                // Lên kệ ~60% nhưng KHÔNG vượt sức chứa kệ — phần dư để trong kho (đúng thực tế).
                int cap = shelf.getCapacity() != null ? shelf.getCapacity() : 0;
                int[] load = shelfLoad.computeIfAbsent(shelf.getId(), k -> new int[]{0});
                int room = cap > 0 ? cap - load[0] : Integer.MAX_VALUE;
                if (room <= 0) continue; // kệ đã đầy → để toàn bộ trong kho
                int want = Math.max(1, (int) Math.round(it.getQuantity() * 0.6));
                int shelfQty = Math.min(Math.min(want, it.getQuantity()), room);
                if (shelfQty <= 0) continue;
                ShelfTransfer st = new ShelfTransfer();
                st.setBatch(it); st.setShelf(shelf); st.setQuantity(shelfQty); st.setCreatedBy(createdBy);
                shelfTransferRepository.save(st);
                load[0] += shelfQty;
            }
        }

        // Lịch sử bán hàng 12 tuần → Dashboard/Báo cáo/ABC-XYZ của chi nhánh có số liệu thật.
        int invoicesSeeded = seedSalesHistory(store, createdBy, cashier, shelves, productsWithStock, randomSeed);
        log.info("Chi nhánh {}: seed tồn kho + lên kệ (tôn trọng sức chứa); tạo {} hóa đơn lịch sử 12 tuần.",
                store.getCode(), invoicesSeeded);
    }

    // ===================== LỊCH SỬ BÁN HÀNG =====================

    /** Số ngày lịch sử demo (12 tuần) — đủ "rổ" tuần để XYZ phân loại được X/Y/Z thay vì dồn hết vào Z. */
    private static final int HISTORY_DAYS = 84;

    private int seedSalesHistory(Store store, User createdBy, User cashier, Map<String, Shelf> shelves,
                                 Map<Long, Product> productsWithStock, long randomSeed) {
        List<Long> productIds = new ArrayList<>(productsWithStock.keySet());
        if (productIds.isEmpty()) return 0;
        Map<Long, int[]> shelfRemain = new HashMap<>();   // batchId (lô lịch sử) -> [còn lại để bán]

        Random rnd = new Random(randomSeed); // tái lập được

        // ---- Hồ sơ nhu cầu mỗi mặt hàng (để ABC × XYZ có phổ thật) ----
        //  pop    : mức phổ biến (Pareto: vài SP bán chạy, đa số bán ít) → quyết định DOANH THU (ABC)
        //  pSell  : xác suất phát sinh bán trong 1 ngày → cao thì bán ĐỀU (CV thấp → X/Y),
        //           thấp thì THẤT THƯỜNG (CV cao → Z). Trộn thêm độ-đều "độc lập" để có cả AZ, CX…
        //  meanQty: số lượng trung bình mỗi ngày-có-bán
        Map<Long, Double> pSell = new HashMap<>();
        Map<Long, Double> meanQty = new HashMap<>();
        for (Long pid : productIds) {
            double pop = Math.pow(rnd.nextDouble(), 1.7) + 0.04;     // độ phổ biến → quyết định DOANH THU (ABC)
            meanQty.put(pid, 1.0 + 5.0 * pop);
            // Gán "tầng độ đều" theo tỉ lệ cố định để bảng có đủ X/Y/Z (không phó mặc may rủi):
            //  ~45% bán ĐỀU (→ X), ~35% DAO ĐỘNG (→ Y), ~20% THẤT THƯỜNG (→ Z).
            double t = rnd.nextDouble();
            double p = t < 0.45 ? 0.70 + 0.25 * rnd.nextDouble()    // bán gần như mỗi ngày → CV thấp
                     : t < 0.80 ? 0.32 + 0.30 * rnd.nextDouble()    // bán cách quãng → CV vừa
                     :            0.07 + 0.06 * rnd.nextDouble();   // bán lẻ tẻ vài lần/tháng → CV cao
            pSell.put(pid, p);
        }

        // ---- Sinh chuỗi nhu cầu theo ngày + tổng nhu cầu mỗi SP ----
        // d = 0 (HISTORY_DAYS ngày trước) … HISTORY_DAYS (hôm nay) để Dashboard có "doanh thu hôm nay".
        List<List<long[]>> demandByDay = new ArrayList<>();
        Map<Long, Integer> totalDemand = new HashMap<>();
        for (int d = 0; d <= HISTORY_DAYS; d++) {
            List<long[]> today = new ArrayList<>();
            for (Long pid : productIds) {
                if (rnd.nextDouble() >= pSell.get(pid)) continue;
                double m = meanQty.get(pid);
                int q = (int) Math.max(1, Math.round(m * (0.4 + 1.2 * rnd.nextDouble())));
                today.add(new long[]{pid, q});
                totalDemand.merge(pid, q, Integer::sum);
            }
            demandByDay.add(today);
        }

        // ---- Lô "cung ứng lịch sử" riêng: số lượng = ĐÚNG tổng nhu cầu → bán hết về 0, KHÔNG đụng tồn
        //      hiện tại (giữ nguyên bức tranh tồn kho/cận HSD/cần nhập của các lô đang bày bán). ----
        Supplier supplier = supplierRepository.findAll().stream().findFirst().orElse(null);
        GoodsReceipt histReceipt = new GoodsReceipt();
        histReceipt.setStore(store);
        histReceipt.setCode(nextReceiptCode());
        histReceipt.setSupplier(supplier);
        histReceipt.setCreatedBy(createdBy);
        histReceipt.setNote("Cung ứng cho lịch sử bán hàng 12 tuần (demo)");
        BigDecimal histTotal = BigDecimal.ZERO;
        Map<Long, GoodsReceiptItem> histBatch = new HashMap<>();
        for (Long pid : productIds) {
            int need = totalDemand.getOrDefault(pid, 0);
            if (need <= 0) continue;
            Product prod = productsWithStock.get(pid);
            GoodsReceiptItem hb = new GoodsReceiptItem();
            hb.setProduct(prod);
            hb.setQuantity(need);
            hb.setImportPrice(prod.getCostPrice());
            hb.setExpiryDate(null); // lô lịch sử: lên kệ rồi bán hết → tồn về 0, không ảnh hưởng HSD/cảnh báo
            histReceipt.addItem(hb);
            histBatch.put(pid, hb);
            histTotal = histTotal.add(prod.getCostPrice().multiply(BigDecimal.valueOf(need)));
        }
        histReceipt.setTotalAmount(histTotal);
        goodsReceiptRepository.save(histReceipt); // cascade → lô có id

        // Đưa lô lịch sử LÊN KỆ đúng bằng số sẽ bán (transferred = sold) → on_shelf = 0 và in_warehouse = 0:
        // lịch sử bán KHÔNG làm tồn kệ/kho âm hay phình, giữ nguyên bức tranh tồn của các lô đang bày bán.
        for (var e : histBatch.entrySet()) {
            GoodsReceiptItem hb = e.getValue();
            shelfRemain.put(hb.getId(), new int[]{hb.getQuantity()});
            Shelf shelf = shelves.get(hb.getProduct().getCategory().getName());
            if (shelf == null) continue;
            ShelfTransfer st = new ShelfTransfer();
            st.setBatch(hb); st.setShelf(shelf); st.setQuantity(hb.getQuantity()); st.setCreatedBy(createdBy);
            shelfTransferRepository.save(st);
        }

        // ---- Bán theo ngày: gói nhu cầu của ngày thành các hóa đơn 1..4 dòng ----
        long invSeq = 0;
        int totalInvoices = 0;
        List<long[]> shiftBackdate = new ArrayList<>();   // [shiftId, dayOffset]
        List<long[]> invBackdate = new ArrayList<>();      // [invoiceId, dayOffset, minuteOfDay]
        for (int d = 0; d <= HISTORY_DAYS; d++) {
            int dayOffset = HISTORY_DAYS - d; // d=HISTORY_DAYS → hôm nay (offset 0)
            WorkShift shift = new WorkShift();
            shift.setStore(store);
            shift.setUser(cashier);
            shift.setOpeningCash(BigDecimal.valueOf(500000));
            shift.setStatus(ShiftStatus.CLOSED);
            shift = workShiftRepository.save(shift);
            shiftBackdate.add(new long[]{shift.getId(), dayOffset});
            BigDecimal cashInDrawer = BigDecimal.valueOf(500000);

            List<long[]> demand = new ArrayList<>(demandByDay.get(d));
            Collections.shuffle(demand, rnd);
            int i = 0;
            while (i < demand.size()) {
                int lines = 1 + rnd.nextInt(4);
                List<long[]> basket = demand.subList(i, Math.min(i + lines, demand.size()));
                i += basket.size();
                Invoice inv = buildInvoiceFromDemand(rnd, shift, basket, shelfRemain, histBatch, ++invSeq, store.getCode());
                if (inv == null) continue;
                Invoice saved = invoiceRepository.save(inv);
                // chỉ tiền mặt mới vào két (QR vào ngân hàng)
                if (saved.getPaymentMethod() == PaymentMethod.CASH) cashInDrawer = cashInDrawer.add(saved.getSubtotal());
                int minute = 8 * 60 + rnd.nextInt(13 * 60); // 8h..21h
                invBackdate.add(new long[]{saved.getId(), dayOffset, minute});
                totalInvoices++;
            }
            shift.setClosingCash(cashInDrawer);
        }
        invoiceRepository.flush();

        // Backdate created_at / opened_at / closed_at (vì @CreationTimestamp không cho set qua JPA)
        em.createNativeQuery("UPDATE goods_receipts SET created_at = :t WHERE id = :id")
                .setParameter("t", LocalDateTime.now().minusDays(HISTORY_DAYS + 1L).withHour(7).withMinute(0).withSecond(0).withNano(0))
                .setParameter("id", histReceipt.getId()).executeUpdate();
        for (long[] s : shiftBackdate) {
            em.createNativeQuery("UPDATE work_shifts SET opened_at = :o, closed_at = :c WHERE id = :id")
                    .setParameter("o", LocalDateTime.now().minusDays(s[1]).withHour(8).withMinute(0).withSecond(0).withNano(0))
                    .setParameter("c", LocalDateTime.now().minusDays(s[1]).withHour(21).withMinute(30).withSecond(0).withNano(0))
                    .setParameter("id", s[0]).executeUpdate();
        }
        for (long[] iv : invBackdate) {
            LocalDateTime ts = LocalDateTime.now().minusDays(iv[1]).withHour(0).withMinute(0).withSecond(0).withNano(0).plusMinutes(iv[2]);
            em.createNativeQuery("UPDATE invoices SET created_at = :t WHERE id = :id")
                    .setParameter("t", ts).setParameter("id", iv[0]).executeUpdate();
        }
        return totalInvoices;
    }

    /** Dựng 1 hóa đơn từ giỏ nhu cầu đã định sẵn (SP + số lượng), lấy hàng từ lô cung ứng lịch sử. */
    private Invoice buildInvoiceFromDemand(Random rnd, WorkShift shift, List<long[]> basket,
                                           Map<Long, int[]> shelfRemain, Map<Long, GoodsReceiptItem> histBatch,
                                           long seq, String storeCode) {
        Invoice inv = new Invoice();
        inv.setShift(shift);
        inv.setStore(shift.getStore());
        BigDecimal subtotal = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        for (long[] line : basket) {
            GoodsReceiptItem b = histBatch.get(line[0]);
            if (b == null) continue;
            int[] rem = shelfRemain.get(b.getId());
            int take = rem == null ? 0 : Math.min(rem[0], (int) line[1]);
            if (take <= 0) continue;
            rem[0] -= take;
            Product prod = b.getProduct();
            InvoiceItem item = new InvoiceItem();
            item.setProduct(prod);
            item.setQuantity(take);
            item.setUnitPrice(prod.getSalePrice());
            InvoiceItemBatch alloc = new InvoiceItemBatch();
            alloc.setBatch(b); alloc.setQuantity(take);
            item.addBatch(alloc);
            inv.addItem(item);
            BigDecimal lineAmt = prod.getSalePrice().multiply(BigDecimal.valueOf(take));
            subtotal = subtotal.add(lineAmt);
            BigDecimal r = prod.getTaxRate();
            if (r != null && r.signum() > 0) {
                tax = tax.add(lineAmt.multiply(r).divide(BigDecimal.valueOf(100).add(r), 2, RoundingMode.HALF_UP));
            }
        }
        if (inv.getItems().isEmpty()) return null;
        inv.setSubtotal(subtotal);
        inv.setDiscountAmount(BigDecimal.ZERO);
        inv.setTaxAmount(tax);
        boolean qr = rnd.nextInt(100) < 25; // ~25% thanh toán QR
        inv.setPaymentMethod(qr ? PaymentMethod.QR : PaymentMethod.CASH);
        if (!qr) {
            BigDecimal paid = subtotal.divide(BigDecimal.valueOf(10000), 0, RoundingMode.CEILING).multiply(BigDecimal.valueOf(10000));
            inv.setCustomerPaid(paid);
            inv.setChangeAmount(paid.subtract(subtotal));
        }
        inv.setPointsEarned(0);
        inv.setPointsUsed(0);
        inv.setStatus(InvoiceStatus.COMPLETED);
        inv.setCode(String.format("HDS-%s-%05d", storeCode, seq));
        return inv;
    }

    // ===================== SUY RA THUẾ / QUY CÁCH / NCC =====================

    private int taxOf(String category) {
        return switch (category) {
            case "Chăm sóc cá nhân", "Hàng tiêu dùng", "Văn phòng phẩm", "Bia & nước có cồn" -> 10;
            default -> 8;
        };
    }

    /** [packSize, dấu hiệu đơn vị: 24=Thùng, 1=không]. */
    private int[] packOf(String category, String unit) {
        boolean drink = category.equals("Nước giải khát") || category.equals("Bia & nước có cồn") || category.equals("Sữa & chế phẩm");
        if (drink && (unit.equals("Lon") || unit.equals("Chai"))) return new int[]{24, 24};
        return new int[]{1, 1};
    }

    private int supplierOf(String category) {
        return switch (category) {
            case "Nước giải khát", "Bia & nước có cồn" -> 0;
            case "Sữa & chế phẩm", "Đồ đông lạnh" -> 2;
            case "Chăm sóc cá nhân", "Hàng tiêu dùng", "Văn phòng phẩm" -> 3;
            default -> 1; // bánh kẹo, đồ ăn nhanh, mì, gia vị
        };
    }

    // ===================== ENSURE MASTER DATA =====================

    private List<Supplier> ensureSuppliers() {
        String[][] defs = {
                {"Nhà phân phối Đồ uống Miền Nam", "02839991111", "douong@npp.vn", "KCN Tân Bình, TP.HCM"},
                {"Công ty Thực phẩm Khô & Bánh kẹo Sài Gòn", "02839992222", "thucpham@npp.vn", "Q.Bình Tân, TP.HCM"},
                {"Nhà cung cấp Sữa & Hàng đông lạnh", "02839993333", "lanhsua@npp.vn", "Q.7, TP.HCM"},
                {"Đại lý Hóa mỹ phẩm & Tiêu dùng", "02839994444", "tieudung@npp.vn", "Q.Tân Phú, TP.HCM"},
        };
        List<Supplier> result = new ArrayList<>();
        for (String[] d : defs) {
            Supplier s = supplierRepository.findAll().stream()
                    .filter(x -> d[0].equalsIgnoreCase(x.getName())).findFirst()
                    .orElseGet(() -> {
                        Supplier x = new Supplier();
                        x.setName(d[0]); x.setPhone(d[1]); x.setEmail(d[2]); x.setAddress(d[3]);
                        x.setStatus(CommonStatus.ACTIVE);
                        return supplierRepository.save(x);
                    });
            result.add(s);
        }
        return result;
    }

    private Map<String, Shelf> ensureShelves(Store store) {
        String[] cats = CAT_NAMES;
        Map<String, Shelf> map = new LinkedHashMap<>();
        for (int i = 0; i < cats.length; i++) {
            String code = String.format("K%02d", i + 1);
            String catName = cats[i];
            Shelf shelf = shelfRepository.findByStoreIdAndCodeIgnoreCase(store.getId(), code).orElseGet(() -> {
                Shelf s = new Shelf();
                s.setStore(store);
                s.setCode(code); s.setName(catName); s.setCapacity(800); s.setStatus(CommonStatus.ACTIVE);
                return shelfRepository.save(s);
            });
            map.put(catName, shelf);
        }
        return map;
    }

    private static final String[] CAT_NAMES = {
            "Nước giải khát", "Đồ ăn nhanh", "Hàng tiêu dùng", "Đồ đông lạnh",
            "Sữa & chế phẩm", "Bánh kẹo", "Mì & ăn liền", "Gia vị & đồ khô",
            "Chăm sóc cá nhân", "Văn phòng phẩm", "Bia & nước có cồn"};

    private Map<String, Category> ensureCategories() {
        Map<String, Category> map = new LinkedHashMap<>();
        for (Category c : categoryRepository.findAll()) map.put(c.getName(), c);
        for (String n : CAT_NAMES) map.computeIfAbsent(n, name -> {
            Category c = new Category();
            c.setName(name); c.setStatus(CommonStatus.ACTIVE);
            return categoryRepository.save(c);
        });
        return map;
    }

    private Map<String, Unit> ensureUnits() {
        List<String> names = List.of("Lon", "Chai", "Gói", "Thùng", "Hộp", "Túi", "Vỉ", "Lốc", "Cây", "Cái");
        Map<String, Unit> map = new LinkedHashMap<>();
        for (Unit u : unitRepository.findAll()) map.put(u.getName(), u);
        for (String n : names) map.computeIfAbsent(n, name -> {
            Unit u = new Unit(); u.setName(name); return unitRepository.save(u);
        });
        return map;
    }

    private String nextReceiptCode() {
        long n = goodsReceiptRepository.countByCodeStartingWith("PN-SEED-") + 1;
        return String.format("PN-SEED-%03d", n);
    }

    /** ~130 mặt hàng tiện lợi điển hình. Vài mặt hàng để tồn thấp/hết & cận HSD để minh hoạ cảnh báo. */
    private List<Spec> specs() {
        List<Spec> s = new ArrayList<>();
        // ----- Nước giải khát -----
        s.add(new Spec("Pepsi lon 330ml", "Nước giải khát", "Lon", 6500, 10000, 24, 240, 300));
        s.add(new Spec("Coca-Cola lon 330ml", "Nước giải khát", "Lon", 6500, 10000, 24, 240, 300));
        s.add(new Spec("7Up lon 330ml", "Nước giải khát", "Lon", 6500, 10000, 24, 192, 300));
        s.add(new Spec("Mirinda xá xị lon 330ml", "Nước giải khát", "Lon", 6500, 10000, 24, 120, 300));
        s.add(new Spec("Sting dâu lon 330ml", "Nước giải khát", "Lon", 7000, 11000, 24, 160, 300));
        s.add(new Spec("Red Bull lon 250ml", "Nước giải khát", "Lon", 9000, 14000, 18, 120, 300));
        s.add(new Spec("Number 1 chai 330ml", "Nước giải khát", "Chai", 6000, 9000, 24, 8, 200));
        s.add(new Spec("Trà Ô Long Tea+ 455ml", "Nước giải khát", "Chai", 7000, 11000, 20, 140, 180));
        s.add(new Spec("Trà xanh 0 độ 455ml", "Nước giải khát", "Chai", 6500, 10000, 20, 150, 180));
        s.add(new Spec("Nước suối Aquafina 500ml", "Nước giải khát", "Chai", 2500, 5000, 48, 360, 540));
        s.add(new Spec("Nước khoáng Lavie 500ml", "Nước giải khát", "Chai", 2800, 5000, 48, 300, 540));
        s.add(new Spec("Nước yến Sanest 190ml", "Nước giải khát", "Lon", 12000, 18000, 12, 60, 365));
        s.add(new Spec("Nước cam Twister 455ml", "Nước giải khát", "Chai", 7000, 11000, 18, 0, 150));
        s.add(new Spec("Trà bí đao Wonderfarm 310ml", "Nước giải khát", "Lon", 6000, 9000, 18, 90, 240));
        s.add(new Spec("Cà phê lon Highlands 235ml", "Nước giải khát", "Lon", 9000, 14000, 18, 72, 270));
        s.add(new Spec("Cà phê Birdy lon 170ml", "Nước giải khát", "Lon", 7000, 11000, 18, 60, 270));
        s.add(new Spec("Nước tăng lực Monster 355ml", "Nước giải khát", "Lon", 14000, 20000, 12, 48, 300));
        s.add(new Spec("Pocari Sweat 500ml", "Nước giải khát", "Chai", 10000, 15000, 18, 12, 240));

        // ----- Sữa & chế phẩm -----
        s.add(new Spec("Sữa tươi Vinamilk có đường 180ml", "Sữa & chế phẩm", "Hộp", 6000, 9000, 30, 200, 90));
        s.add(new Spec("Sữa tươi TH True Milk 180ml", "Sữa & chế phẩm", "Hộp", 6500, 9500, 30, 6, 90));
        s.add(new Spec("Sữa tươi Dutch Lady 180ml", "Sữa & chế phẩm", "Hộp", 6000, 9000, 30, 150, 90));
        s.add(new Spec("Sữa chua uống Yakult lốc 5", "Sữa & chế phẩm", "Lốc", 22000, 30000, 16, 60, 40));
        s.add(new Spec("Sữa chua Vinamilk có đường", "Sữa & chế phẩm", "Hộp", 5000, 7500, 24, 120, 30));
        s.add(new Spec("Sữa chua nếp cẩm", "Sữa & chế phẩm", "Hộp", 6000, 9000, 24, 10, 25));
        s.add(new Spec("Sữa Milo lon 180ml", "Sữa & chế phẩm", "Lon", 7000, 10000, 24, 96, 200));
        s.add(new Spec("Phô mai Con Bò Cười 8 miếng", "Sữa & chế phẩm", "Hộp", 22000, 32000, 12, 40, 180));
        s.add(new Spec("Sữa đặc Ông Thọ 380g", "Sữa & chế phẩm", "Lon", 18000, 25000, 18, 70, 365));
        s.add(new Spec("Sữa bột Ensure Gold 400g", "Sữa & chế phẩm", "Hộp", 250000, 320000, 6, 0, 540));
        s.add(new Spec("Sữa đậu nành Fami 200ml", "Sữa & chế phẩm", "Hộp", 4500, 7000, 30, 180, 120));
        s.add(new Spec("Bơ lạt Anchor 100g", "Sữa & chế phẩm", "Hộp", 35000, 48000, 10, 18, 200));

        // ----- Bánh kẹo -----
        s.add(new Spec("Bánh Oreo 119g", "Bánh kẹo", "Gói", 9000, 14000, 24, 100, 180));
        s.add(new Spec("Bánh Chocopie hộp 12 cái", "Bánh kẹo", "Hộp", 38000, 52000, 12, 50, 200));
        s.add(new Spec("Bánh quy Cosy marie 200g", "Bánh kẹo", "Gói", 12000, 18000, 18, 70, 240));
        s.add(new Spec("Kẹo dẻo Haribo 80g", "Bánh kẹo", "Gói", 15000, 22000, 18, 55, 270));
        s.add(new Spec("Socola KitKat 4 thanh", "Bánh kẹo", "Gói", 12000, 18000, 18, 7, 240));
        s.add(new Spec("Socola M&M 40g", "Bánh kẹo", "Gói", 14000, 20000, 18, 40, 240));
        s.add(new Spec("Bánh gạo One One 150g", "Bánh kẹo", "Gói", 11000, 16000, 18, 85, 200));
        s.add(new Spec("Kẹo Mentos bạc hà", "Bánh kẹo", "Cây", 6000, 9000, 24, 120, 360));
        s.add(new Spec("Bánh Custas 6 cái", "Bánh kẹo", "Hộp", 22000, 30000, 12, 0, 150));
        s.add(new Spec("Bánh AFC vị rau 200g", "Bánh kẹo", "Gói", 11000, 16000, 18, 60, 200));
        s.add(new Spec("Kẹo singum Doublemint", "Bánh kẹo", "Vỉ", 5000, 8000, 24, 100, 360));
        s.add(new Spec("Bánh trung thu Kinh Đô (lẻ)", "Bánh kẹo", "Cái", 35000, 50000, 10, 12, 20));

        // ----- Snack / Đồ ăn nhanh -----
        s.add(new Spec("Snack Lays vị tự nhiên 52g", "Đồ ăn nhanh", "Gói", 7000, 11000, 24, 110, 150));
        s.add(new Spec("Snack Poca khoai tây 48g", "Đồ ăn nhanh", "Gói", 6000, 9000, 24, 100, 150));
        s.add(new Spec("Snack O'Star vị bò 50g", "Đồ ăn nhanh", "Gói", 6000, 9000, 24, 80, 150));
        s.add(new Spec("Bắp rang bơ 60g", "Đồ ăn nhanh", "Gói", 8000, 12000, 18, 50, 120));
        s.add(new Spec("Rong biển Tao Kae Noi 32g", "Đồ ăn nhanh", "Gói", 18000, 26000, 12, 30, 180));
        s.add(new Spec("Hạt điều rang muối 100g", "Đồ ăn nhanh", "Túi", 28000, 40000, 12, 5, 120));
        s.add(new Spec("Hạt hướng dương 100g", "Đồ ăn nhanh", "Túi", 12000, 18000, 18, 60, 150));
        s.add(new Spec("Xúc xích Vissan tiệt trùng", "Đồ ăn nhanh", "Cây", 4000, 6000, 30, 140, 90));
        s.add(new Spec("Bánh mì tươi ngọt", "Đồ ăn nhanh", "Cái", 5000, 8000, 20, 14, 3));
        s.add(new Spec("Khô bò miếng 50g", "Đồ ăn nhanh", "Gói", 22000, 32000, 12, 25, 120));

        // ----- Mì & ăn liền -----
        s.add(new Spec("Mì Omachi sốt bò hầm", "Mì & ăn liền", "Gói", 5000, 7500, 36, 300, 200));
        s.add(new Spec("Mì Hảo Hảo tôm chua cay", "Mì & ăn liền", "Gói", 3000, 4500, 48, 360, 200));
        s.add(new Spec("Mì 3 Miền tôm chua cay", "Mì & ăn liền", "Gói", 2800, 4000, 40, 280, 200));
        s.add(new Spec("Phở ăn liền Vifon bò", "Mì & ăn liền", "Gói", 6000, 9000, 24, 110, 180));
        s.add(new Spec("Cháo gói Gấu Đỏ", "Mì & ăn liền", "Gói", 3500, 5500, 30, 130, 180));
        s.add(new Spec("Hủ tiếu Nam Vang ăn liền", "Mì & ăn liền", "Gói", 6500, 9500, 18, 9, 180));
        s.add(new Spec("Mì ly Modern Lẩu Thái", "Mì & ăn liền", "Hộp", 7000, 10500, 24, 90, 200));
        s.add(new Spec("Miến Phú Hương 58g", "Mì & ăn liền", "Gói", 5500, 8000, 18, 0, 180));
        s.add(new Spec("Mì Kokomi đại 90g", "Mì & ăn liền", "Gói", 3500, 5000, 40, 200, 200));
        s.add(new Spec("Bún bò Huế ăn liền", "Mì & ăn liền", "Gói", 7000, 10000, 18, 40, 180));

        // ----- Gia vị & đồ khô -----
        s.add(new Spec("Nước mắm Nam Ngư 500ml", "Gia vị & đồ khô", "Chai", 22000, 30000, 18, 70, 540));
        s.add(new Spec("Nước tương Maggi 300ml", "Gia vị & đồ khô", "Chai", 14000, 20000, 18, 65, 540));
        s.add(new Spec("Dầu ăn Neptune 1L", "Gia vị & đồ khô", "Chai", 42000, 55000, 12, 40, 540));
        s.add(new Spec("Đường trắng Biên Hòa 1kg", "Gia vị & đồ khô", "Túi", 20000, 27000, 18, 60, 720));
        s.add(new Spec("Muối I-ốt 500g", "Gia vị & đồ khô", "Túi", 4000, 6000, 18, 90, 720));
        s.add(new Spec("Hạt nêm Knorr 400g", "Gia vị & đồ khô", "Gói", 28000, 38000, 12, 6, 540));
        s.add(new Spec("Tương ớt Chinsu 250g", "Gia vị & đồ khô", "Chai", 12000, 17000, 18, 75, 540));
        s.add(new Spec("Tương cà Cholimex 270g", "Gia vị & đồ khô", "Chai", 11000, 16000, 18, 55, 540));
        s.add(new Spec("Gạo ST25 túi 5kg", "Gia vị & đồ khô", "Túi", 130000, 165000, 8, 25, 365));
        s.add(new Spec("Bột ngọt Ajinomoto 400g", "Gia vị & đồ khô", "Gói", 25000, 33000, 12, 35, 540));
        s.add(new Spec("Cà phê hòa tan G7 hộp 21", "Gia vị & đồ khô", "Hộp", 38000, 52000, 12, 28, 365));
        s.add(new Spec("Trà Lipton nhãn vàng 25 gói", "Gia vị & đồ khô", "Hộp", 22000, 32000, 12, 8, 365));

        // ----- Đồ đông lạnh -----
        s.add(new Spec("Kem Wall's Cornetto", "Đồ đông lạnh", "Cái", 8000, 13000, 18, 70, 365));
        s.add(new Spec("Kem Merino ốc quế", "Đồ đông lạnh", "Cái", 7000, 11000, 18, 60, 365));
        s.add(new Spec("Kem que Celano", "Đồ đông lạnh", "Cái", 6000, 10000, 18, 55, 365));
        s.add(new Spec("Há cảo CP gói 200g", "Đồ đông lạnh", "Gói", 28000, 38000, 12, 24, 270));
        s.add(new Spec("Chả giò Cầu Tre 500g", "Đồ đông lạnh", "Gói", 35000, 48000, 12, 3, 270));
        s.add(new Spec("Xúc xích đông lạnh Đức Việt", "Đồ đông lạnh", "Gói", 30000, 42000, 12, 30, 180));
        s.add(new Spec("Cá viên CP 500g", "Đồ đông lạnh", "Gói", 32000, 44000, 12, 20, 240));
        s.add(new Spec("Khoai tây chiên đông lạnh 500g", "Đồ đông lạnh", "Gói", 28000, 39000, 12, 7, 270));

        // ----- Chăm sóc cá nhân -----
        s.add(new Spec("Dầu gội Clear bạc hà 170g", "Chăm sóc cá nhân", "Chai", 38000, 52000, 12, 35, 720));
        s.add(new Spec("Dầu gội Sunsilk 170g", "Chăm sóc cá nhân", "Chai", 32000, 45000, 12, 30, 720));
        s.add(new Spec("Kem đánh răng P/S 180g", "Chăm sóc cá nhân", "Hộp", 18000, 26000, 18, 50, 720));
        s.add(new Spec("Bàn chải Colgate", "Chăm sóc cá nhân", "Cái", 9000, 15000, 18, 80, 0));
        s.add(new Spec("Sữa tắm Lifebuoy 170g", "Chăm sóc cá nhân", "Chai", 32000, 45000, 12, 7, 720));
        s.add(new Spec("Khẩu trang y tế hộp 50 cái", "Chăm sóc cá nhân", "Hộp", 25000, 38000, 12, 40, 0));
        s.add(new Spec("Khăn giấy Pulppy 4 lớp", "Chăm sóc cá nhân", "Gói", 8000, 12000, 24, 120, 0));
        s.add(new Spec("Băng vệ sinh Diana 8 miếng", "Chăm sóc cá nhân", "Gói", 14000, 20000, 18, 45, 720));
        s.add(new Spec("Dao cạo râu Gillette", "Chăm sóc cá nhân", "Cái", 12000, 19000, 18, 30, 0));
        s.add(new Spec("Nước rửa tay Lifebuoy 180ml", "Chăm sóc cá nhân", "Chai", 22000, 32000, 12, 6, 720));

        // ----- Hàng tiêu dùng / hoá phẩm -----
        s.add(new Spec("Nước rửa chén Sunlight 750ml", "Hàng tiêu dùng", "Chai", 18000, 26000, 12, 45, 720));
        s.add(new Spec("Bột giặt Omo 400g", "Hàng tiêu dùng", "Túi", 22000, 30000, 12, 40, 720));
        s.add(new Spec("Nước lau sàn Sunlight 1L", "Hàng tiêu dùng", "Chai", 25000, 35000, 12, 4, 720));
        s.add(new Spec("Nước xả Comfort 1L", "Hàng tiêu dùng", "Chai", 30000, 42000, 12, 25, 720));
        s.add(new Spec("Túi rác cuộn 3 cuộn", "Hàng tiêu dùng", "Lốc", 15000, 22000, 12, 50, 0));
        s.add(new Spec("Pin Con Ó AA vỉ 2 viên", "Hàng tiêu dùng", "Vỉ", 8000, 13000, 18, 60, 0));
        s.add(new Spec("Bật lửa ga", "Hàng tiêu dùng", "Cái", 3000, 6000, 24, 100, 0));
        s.add(new Spec("Nến cây thường", "Hàng tiêu dùng", "Cây", 2000, 4000, 24, 80, 0));
        s.add(new Spec("Băng keo trong", "Hàng tiêu dùng", "Cái", 4000, 7000, 18, 9, 0));

        // ----- Văn phòng phẩm -----
        s.add(new Spec("Bút bi Thiên Long TL-027", "Văn phòng phẩm", "Cây", 3000, 5000, 30, 150, 0));
        s.add(new Spec("Bút chì gỗ 2B", "Văn phòng phẩm", "Cây", 2500, 4000, 30, 120, 0));
        s.add(new Spec("Tập học sinh 96 trang", "Văn phòng phẩm", "Cái", 6000, 9000, 24, 100, 0));
        s.add(new Spec("Tập học sinh 200 trang", "Văn phòng phẩm", "Cái", 10000, 15000, 24, 80, 0));
        s.add(new Spec("Gôm tẩy Thiên Long", "Văn phòng phẩm", "Cái", 2000, 4000, 24, 90, 0));
        s.add(new Spec("Thước kẻ 20cm", "Văn phòng phẩm", "Cái", 3000, 6000, 18, 5, 0));
        s.add(new Spec("Bút xóa nước", "Văn phòng phẩm", "Cây", 7000, 12000, 18, 40, 0));

        // ----- Bia & nước có cồn -----
        s.add(new Spec("Bia Tiger lon 330ml", "Bia & nước có cồn", "Lon", 12000, 17000, 24, 240, 365));
        s.add(new Spec("Bia Heineken lon 330ml", "Bia & nước có cồn", "Lon", 15000, 20000, 24, 180, 365));
        s.add(new Spec("Bia Saigon Special lon 330ml", "Bia & nước có cồn", "Lon", 11000, 15000, 24, 200, 365));
        s.add(new Spec("Bia 333 lon 330ml", "Bia & nước có cồn", "Lon", 10000, 14000, 24, 8, 365));
        s.add(new Spec("Bia Budweiser lon 330ml", "Bia & nước có cồn", "Lon", 16000, 22000, 18, 60, 365));
        s.add(new Spec("Nước trái cây lên men Strongbow 320ml", "Bia & nước có cồn", "Lon", 18000, 25000, 18, 36, 270));
        return s;
    }
}
