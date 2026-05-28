package com.pos.config;

import com.pos.entity.*;
import com.pos.entity.enums.CommonStatus;
import com.pos.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seed danh mục sản phẩm phong phú cho demo/đồ án (≈ 60+ mặt hàng) kèm tồn kho thật.
 *
 * <p>Idempotent: chỉ thêm những gì còn thiếu (danh mục/đơn vị/NCC theo tên, sản phẩm theo
 * mã vạch). Chạy lại nhiều lần không nhân bản dữ liệu. Mỗi sản phẩm mới được nhập 1 lô
 * (goods_receipt_items) để view tồn kho có số liệu — vài mặt hàng cố ý để tồn thấp / hết
 * hàng nhằm minh hoạ cảnh báo kho và tính năng đề xuất nhập hàng.</p>
 *
 * <p>Chỉ chạy khi profile KHÁC prod.</p>
 */
@Component
@Profile("!prod")
@Order(20) // sau DemoDataInitializer (mật khẩu demo)
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

    public CatalogDemoDataInitializer(CategoryRepository categoryRepository,
                                      UnitRepository unitRepository,
                                      SupplierRepository supplierRepository,
                                      ProductRepository productRepository,
                                      GoodsReceiptRepository goodsReceiptRepository,
                                      UserRepository userRepository,
                                      ShelfTransferRepository shelfTransferRepository,
                                      ShelfRepository shelfRepository) {
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.userRepository = userRepository;
        this.shelfTransferRepository = shelfTransferRepository;
        this.shelfRepository = shelfRepository;
    }

    /** Đặc tả 1 mặt hàng demo. shelfLifeDays = 0 → không có HSD; stockQty = 0 → hết hàng. */
    private record Spec(String name, String category, String unit,
                        long cost, long sale, int minStock, int stockQty, int shelfLifeDays) {}

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Category> categories = ensureCategories();
        Map<String, Unit> units = ensureUnits();
        Map<String, Shelf> shelves = ensureShelves();
        Supplier supplier = ensureDemoSupplier();
        User createdBy = userRepository.findByUsername("manager")
                .or(() -> userRepository.findByUsername("admin"))
                .orElse(null);
        if (createdBy == null) {
            log.warn("Bỏ qua seed catalog: chưa có tài khoản manager/admin để gán người lập phiếu nhập.");
            return;
        }

        List<Spec> specs = specs();
        List<GoodsReceiptItem> newBatches = new ArrayList<>();
        List<String> batchCategories = new ArrayList<>(); // danh mục của từng lô (để chọn kệ)
        int created = 0;
        int idx = 0;

        for (Spec s : specs) {
            // Mã vạch ỔN ĐỊNH theo vị trí trong danh sách → chạy lại luôn khớp, KHÔNG tạo trùng.
            String barcode = String.format("8930%09d", ++idx);
            if (productRepository.existsByBarcode(barcode)) {
                continue; // đã seed ở lần chạy trước
            }
            Product p = new Product();
            p.setBarcode(barcode);
            p.setName(s.name());
            p.setCategory(categories.get(s.category()));
            p.setUnit(units.get(s.unit()));
            p.setCostPrice(BigDecimal.valueOf(s.cost()));
            p.setSalePrice(BigDecimal.valueOf(s.sale()));
            p.setMinStock(s.minStock());
            p.setStatus(CommonStatus.ACTIVE);
            productRepository.save(p);
            created++;

            if (s.stockQty() > 0) {
                GoodsReceiptItem item = new GoodsReceiptItem();
                item.setProduct(p);
                item.setQuantity(s.stockQty());
                item.setImportPrice(BigDecimal.valueOf(s.cost()));
                item.setExpiryDate(s.shelfLifeDays() > 0
                        ? LocalDate.now().plusDays(s.shelfLifeDays()) : null);
                newBatches.add(item);
                batchCategories.add(s.category());
            }
        }

        if (created == 0) {
            log.info("Catalog demo đã đầy đủ — không seed thêm.");
            return;
        }

        if (!newBatches.isEmpty()) {
            GoodsReceipt receipt = new GoodsReceipt();
            receipt.setCode(nextReceiptCode());
            receipt.setSupplier(supplier);
            receipt.setCreatedBy(createdBy);
            receipt.setNote("Nhập tồn kho ban đầu (dữ liệu demo)");
            BigDecimal total = BigDecimal.ZERO;
            for (GoodsReceiptItem it : newBatches) {
                receipt.addItem(it);
                total = total.add(it.getImportPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            }
            receipt.setTotalAmount(total);
            goodsReceiptRepository.save(receipt); // cascade lưu lô → lô có id

            // Đưa sẵn ~60% mỗi lô LÊN KỆ của danh mục (phần còn lại nằm trong KHO) — POS bán được + minh hoạ kho/kệ.
            for (int i = 0; i < newBatches.size(); i++) {
                GoodsReceiptItem it = newBatches.get(i);
                Shelf shelf = shelves.get(batchCategories.get(i));
                if (shelf == null) continue;
                int shelfQty = Math.min(it.getQuantity(), Math.max(1, (int) Math.round(it.getQuantity() * 0.6)));
                ShelfTransfer st = new ShelfTransfer();
                st.setBatch(it);
                st.setShelf(shelf);
                st.setQuantity(shelfQty);
                st.setCreatedBy(createdBy);
                shelfTransferRepository.save(st);
            }
        }

        log.info("Đã seed {} sản phẩm demo + {} lô nhập tồn kho, đã lên kệ ~60% vào {} kệ.",
                created, newBatches.size(), shelves.size());
    }

    // ---- helpers --------------------------------------------------------

    /** Mỗi danh mục một KỆ (K01..K10), idempotent theo mã kệ → trả map danh mục → kệ. */
    private Map<String, Shelf> ensureShelves() {
        String[] cats = {
                "Nước giải khát", "Đồ ăn nhanh", "Hàng tiêu dùng", "Đồ đông lạnh",
                "Sữa & chế phẩm", "Bánh kẹo", "Mì & ăn liền", "Gia vị & đồ khô",
                "Chăm sóc cá nhân", "Văn phòng phẩm"};
        Map<String, Shelf> map = new LinkedHashMap<>();
        for (int i = 0; i < cats.length; i++) {
            String code = String.format("K%02d", i + 1);
            String catName = cats[i];
            Shelf shelf = shelfRepository.findByCodeIgnoreCase(code).orElseGet(() -> {
                Shelf s = new Shelf();
                s.setCode(code);
                s.setName(catName);
                s.setCapacity(500); // sức chứa mặc định mỗi kệ
                s.setStatus(CommonStatus.ACTIVE);
                return shelfRepository.save(s);
            });
            map.put(catName, shelf);
        }
        return map;
    }

    private Map<String, Category> ensureCategories() {
        // tên hiển thị (giữ nguyên các danh mục có sẵn trong schema.sql)
        List<String> names = List.of(
                "Nước giải khát", "Đồ ăn nhanh", "Hàng tiêu dùng", "Đồ đông lạnh",
                "Sữa & chế phẩm", "Bánh kẹo", "Mì & ăn liền", "Gia vị & đồ khô",
                "Chăm sóc cá nhân", "Văn phòng phẩm");
        Map<String, Category> map = new LinkedHashMap<>();
        for (Category c : categoryRepository.findAll()) {
            map.put(c.getName(), c);
        }
        for (String n : names) {
            map.computeIfAbsent(n, name -> {
                Category c = new Category();
                c.setName(name);
                c.setStatus(CommonStatus.ACTIVE);
                return categoryRepository.save(c);
            });
        }
        return map;
    }

    private Map<String, Unit> ensureUnits() {
        List<String> names = List.of(
                "Lon", "Chai", "Gói", "Thùng", "Hộp", "Túi", "Vỉ", "Lốc", "Cây", "Cái");
        Map<String, Unit> map = new LinkedHashMap<>();
        for (Unit u : unitRepository.findAll()) {
            map.put(u.getName(), u);
        }
        for (String n : names) {
            map.computeIfAbsent(n, name -> {
                Unit u = new Unit();
                u.setName(name);
                return unitRepository.save(u);
            });
        }
        return map;
    }

    private Supplier ensureDemoSupplier() {
        String name = "Nhà phân phối tổng hợp Miền Nam";
        return supplierRepository.findAll().stream()
                .filter(s -> name.equalsIgnoreCase(s.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Supplier s = new Supplier();
                    s.setName(name);
                    s.setPhone("02839998888");
                    s.setEmail("sales@npptonghop.vn");
                    s.setAddress("KCN Tân Bình, TP.HCM");
                    s.setStatus(CommonStatus.ACTIVE);
                    return supplierRepository.save(s);
                });
    }

    private String nextReceiptCode() {
        long n = goodsReceiptRepository.countByCodeStartingWith("PN-SEED-") + 1;
        return String.format("PN-SEED-%03d", n);
    }

    /** Danh mục ≈ 64 mặt hàng tiện lợi điển hình. Vài mặt hàng để tồn thấp/hết để minh hoạ cảnh báo. */
    private List<Spec> specs() {
        List<Spec> s = new ArrayList<>();
        // ----- Nước giải khát -----
        s.add(new Spec("Pepsi lon 330ml", "Nước giải khát", "Lon", 6500, 10000, 24, 120, 300));
        s.add(new Spec("7Up lon 330ml", "Nước giải khát", "Lon", 6500, 10000, 24, 96, 300));
        s.add(new Spec("Sting dâu lon 330ml", "Nước giải khát", "Lon", 7000, 11000, 24, 80, 300));
        s.add(new Spec("Red Bull lon 250ml", "Nước giải khát", "Lon", 9000, 14000, 18, 60, 300));
        s.add(new Spec("Number 1 chai 330ml", "Nước giải khát", "Chai", 6000, 9000, 24, 8, 200));
        s.add(new Spec("Trà Ô Long Tea+ 455ml", "Nước giải khát", "Chai", 7000, 11000, 20, 70, 180));
        s.add(new Spec("Nước yến Sanest 190ml", "Nước giải khát", "Lon", 12000, 18000, 12, 40, 365));
        s.add(new Spec("Nước cam Twister 455ml", "Nước giải khát", "Chai", 7000, 11000, 18, 0, 150));
        s.add(new Spec("Trà bí đao Wonderfarm 310ml", "Nước giải khát", "Lon", 6000, 9000, 18, 55, 240));
        s.add(new Spec("Cà phê lon Highlands 235ml", "Nước giải khát", "Lon", 9000, 14000, 18, 36, 270));
        s.add(new Spec("Nước tăng lực Monster 355ml", "Nước giải khát", "Lon", 14000, 20000, 12, 24, 300));
        s.add(new Spec("Sữa đậu nành Fami 200ml", "Nước giải khát", "Hộp", 4500, 7000, 30, 150, 120));

        // ----- Sữa & chế phẩm -----
        s.add(new Spec("Sữa tươi Vinamilk có đường 180ml", "Sữa & chế phẩm", "Hộp", 6000, 9000, 30, 200, 120));
        s.add(new Spec("Sữa tươi TH True Milk 180ml", "Sữa & chế phẩm", "Hộp", 6500, 9500, 30, 6, 120));
        s.add(new Spec("Sữa chua uống Yakult lốc 5", "Sữa & chế phẩm", "Lốc", 22000, 30000, 16, 48, 45));
        s.add(new Spec("Sữa chua Vinamilk có đường", "Sữa & chế phẩm", "Hộp", 5000, 7500, 24, 90, 40));
        s.add(new Spec("Sữa Milo lon 180ml", "Sữa & chế phẩm", "Lon", 7000, 10000, 24, 64, 200));
        s.add(new Spec("Phô mai Con Bò Cười 8 miếng", "Sữa & chế phẩm", "Hộp", 22000, 32000, 12, 30, 180));
        s.add(new Spec("Sữa đặc Ông Thọ 380g", "Sữa & chế phẩm", "Lon", 18000, 25000, 18, 50, 365));
        s.add(new Spec("Sữa bột Ensure Gold 400g", "Sữa & chế phẩm", "Hộp", 250000, 320000, 6, 0, 540));

        // ----- Bánh kẹo -----
        s.add(new Spec("Bánh Oreo 119g", "Bánh kẹo", "Gói", 9000, 14000, 24, 80, 180));
        s.add(new Spec("Bánh Chocopie hộp 12 cái", "Bánh kẹo", "Hộp", 38000, 52000, 12, 40, 200));
        s.add(new Spec("Bánh quy Cosy marie 200g", "Bánh kẹo", "Gói", 12000, 18000, 18, 60, 240));
        s.add(new Spec("Kẹo dẻo Haribo 80g", "Bánh kẹo", "Gói", 15000, 22000, 18, 45, 270));
        s.add(new Spec("Socola KitKat 4 thanh", "Bánh kẹo", "Gói", 12000, 18000, 18, 7, 240));
        s.add(new Spec("Bánh gạo One One 150g", "Bánh kẹo", "Gói", 11000, 16000, 18, 70, 200));
        s.add(new Spec("Kẹo Mentos bạc hà", "Bánh kẹo", "Cây", 6000, 9000, 24, 100, 360));
        s.add(new Spec("Bánh Custas 6 cái", "Bánh kẹo", "Hộp", 22000, 30000, 12, 0, 150));

        // ----- Snack / Đồ ăn nhanh -----
        s.add(new Spec("Snack Lays vị tự nhiên 52g", "Đồ ăn nhanh", "Gói", 7000, 11000, 24, 90, 150));
        s.add(new Spec("Snack Poca khoai tây 48g", "Đồ ăn nhanh", "Gói", 6000, 9000, 24, 85, 150));
        s.add(new Spec("Bắp rang bơ 60g", "Đồ ăn nhanh", "Gói", 8000, 12000, 18, 40, 120));
        s.add(new Spec("Rong biển Tao Kae Noi 32g", "Đồ ăn nhanh", "Gói", 18000, 26000, 12, 25, 180));
        s.add(new Spec("Hạt điều rang muối 100g", "Đồ ăn nhanh", "Túi", 28000, 40000, 12, 5, 120));
        s.add(new Spec("Xúc xích Vissan tiệt trùng", "Đồ ăn nhanh", "Cây", 4000, 6000, 30, 120, 90));

        // ----- Mì & ăn liền -----
        s.add(new Spec("Mì Omachi sốt bò hầm", "Mì & ăn liền", "Gói", 5000, 7500, 36, 200, 200));
        s.add(new Spec("Mì 3 Miền tôm chua cay", "Mì & ăn liền", "Gói", 2800, 4000, 40, 240, 200));
        s.add(new Spec("Phở ăn liền Vifon bò", "Mì & ăn liền", "Gói", 6000, 9000, 24, 90, 180));
        s.add(new Spec("Cháo gói Gấu Đỏ", "Mì & ăn liền", "Gói", 3500, 5500, 30, 110, 180));
        s.add(new Spec("Hủ tiếu Nam Vang ăn liền", "Mì & ăn liền", "Gói", 6500, 9500, 18, 9, 180));
        s.add(new Spec("Mì ly Modern Lẩu Thái", "Mì & ăn liền", "Hộp", 7000, 10500, 24, 70, 200));
        s.add(new Spec("Miến Phú Hương 58g", "Mì & ăn liền", "Gói", 5500, 8000, 18, 0, 180));

        // ----- Gia vị & đồ khô -----
        s.add(new Spec("Nước mắm Nam Ngư 500ml", "Gia vị & đồ khô", "Chai", 22000, 30000, 18, 60, 540));
        s.add(new Spec("Nước tương Maggi 300ml", "Gia vị & đồ khô", "Chai", 14000, 20000, 18, 55, 540));
        s.add(new Spec("Dầu ăn Neptune 1L", "Gia vị & đồ khô", "Chai", 42000, 55000, 12, 30, 540));
        s.add(new Spec("Đường trắng Biên Hòa 1kg", "Gia vị & đồ khô", "Túi", 20000, 27000, 18, 50, 720));
        s.add(new Spec("Muối I-ốt 500g", "Gia vị & đồ khô", "Túi", 4000, 6000, 18, 80, 720));
        s.add(new Spec("Hạt nêm Knorr 400g", "Gia vị & đồ khô", "Gói", 28000, 38000, 12, 6, 540));
        s.add(new Spec("Tương ớt Chinsu 250g", "Gia vị & đồ khô", "Chai", 12000, 17000, 18, 65, 540));
        s.add(new Spec("Gạo ST25 túi 5kg", "Gia vị & đồ khô", "Túi", 130000, 165000, 8, 20, 365));

        // ----- Đồ đông lạnh -----
        s.add(new Spec("Kem Wall's Cornetto", "Đồ đông lạnh", "Cái", 8000, 13000, 18, 50, 365));
        s.add(new Spec("Kem Merino ốc quế", "Đồ đông lạnh", "Cái", 7000, 11000, 18, 45, 365));
        s.add(new Spec("Há cảo CP gói 200g", "Đồ đông lạnh", "Gói", 28000, 38000, 12, 18, 270));
        s.add(new Spec("Chả giò Cầu Tre 500g", "Đồ đông lạnh", "Gói", 35000, 48000, 12, 3, 270));
        s.add(new Spec("Xúc xích đông lạnh Đức Việt", "Đồ đông lạnh", "Gói", 30000, 42000, 12, 22, 180));

        // ----- Chăm sóc cá nhân -----
        s.add(new Spec("Dầu gội Clear bạc hà 170g", "Chăm sóc cá nhân", "Chai", 38000, 52000, 12, 25, 720));
        s.add(new Spec("Kem đánh răng P/S 180g", "Chăm sóc cá nhân", "Hộp", 18000, 26000, 18, 40, 720));
        s.add(new Spec("Bàn chải Colgate", "Chăm sóc cá nhân", "Cái", 9000, 15000, 18, 60, 0));
        s.add(new Spec("Sữa tắm Lifebuoy 170g", "Chăm sóc cá nhân", "Chai", 32000, 45000, 12, 7, 720));
        s.add(new Spec("Khẩu trang y tế hộp 50 cái", "Chăm sóc cá nhân", "Hộp", 25000, 38000, 12, 30, 0));
        s.add(new Spec("Khăn giấy Pulppy 4 lớp", "Chăm sóc cá nhân", "Gói", 8000, 12000, 24, 100, 0));

        // ----- Hàng tiêu dùng / hoá phẩm -----
        s.add(new Spec("Nước rửa chén Sunlight 750ml", "Hàng tiêu dùng", "Chai", 18000, 26000, 12, 35, 720));
        s.add(new Spec("Bột giặt Omo 400g", "Hàng tiêu dùng", "Túi", 22000, 30000, 12, 28, 720));
        s.add(new Spec("Nước lau sàn Sunlight 1L", "Hàng tiêu dùng", "Chai", 25000, 35000, 12, 4, 720));
        s.add(new Spec("Túi rác cuộn 3 cuộn", "Hàng tiêu dùng", "Lốc", 15000, 22000, 12, 40, 0));
        s.add(new Spec("Pin Con Ó AA vỉ 2 viên", "Hàng tiêu dùng", "Vỉ", 8000, 13000, 18, 50, 0));
        s.add(new Spec("Bật lửa ga", "Hàng tiêu dùng", "Cái", 3000, 6000, 24, 80, 0));

        // ----- Văn phòng phẩm -----
        s.add(new Spec("Bút bi Thiên Long TL-027", "Văn phòng phẩm", "Cây", 3000, 5000, 30, 120, 0));
        s.add(new Spec("Tập học sinh 96 trang", "Văn phòng phẩm", "Cái", 6000, 9000, 24, 90, 0));
        s.add(new Spec("Băng keo trong", "Văn phòng phẩm", "Cái", 4000, 7000, 18, 10, 0));
        return s;
    }
}
