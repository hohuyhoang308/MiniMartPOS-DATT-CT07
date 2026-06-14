package com.pos.config;

import com.pos.entity.*;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.DiscountType;
import com.pos.entity.enums.Role;
import com.pos.entity.enums.ShiftStatus;
import com.pos.entity.enums.UserStatus;
import com.pos.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seed DỮ LIỆU NỀN bắt buộc để hệ thống chạy được trên CSDL trống (sau khi drop + tạo lại cấu trúc):
 * CHI NHÁNH (đa chuỗi), tài khoản đăng nhập (gắn chi nhánh), cấu hình từng chi nhánh, vài khách &
 * khuyến mãi demo, và 1 ca mở sẵn cho thu ngân chi nhánh 1.
 *
 * <p>Idempotent (chỉ thêm phần còn thiếu) → an toàn chạy lại mỗi lần khởi động. Chỉ chạy khi
 * profile KHÁC prod. Chạy TRƯỚC {@link CatalogDemoDataInitializer} (cần có user/chi nhánh để gán phiếu nhập).</p>
 */
@Component
@Profile("!prod")
@Order(10)
public class BaseDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BaseDataSeeder.class);
    private static final String DEMO_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreConfigRepository storeConfigRepository;
    private final CustomerRepository customerRepository;
    private final PromotionRepository promotionRepository;
    private final WorkShiftRepository shiftRepository;
    private final LoyaltyPointLedgerRepository ledgerRepository;
    private final com.pos.service.LoyaltyService loyaltyService;
    private final PasswordEncoder passwordEncoder;

    public BaseDataSeeder(UserRepository userRepository,
                          StoreRepository storeRepository,
                          StoreConfigRepository storeConfigRepository,
                          CustomerRepository customerRepository,
                          PromotionRepository promotionRepository,
                          WorkShiftRepository shiftRepository,
                          LoyaltyPointLedgerRepository ledgerRepository,
                          com.pos.service.LoyaltyService loyaltyService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.storeConfigRepository = storeConfigRepository;
        this.customerRepository = customerRepository;
        this.promotionRepository = promotionRepository;
        this.shiftRepository = shiftRepository;
        this.ledgerRepository = ledgerRepository;
        this.loyaltyService = loyaltyService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Store ch01 = ensureStore("CH01", "MiniMart — Cửa hàng 1 (Quận 1)", "123 Đường Lê Lợi, Q.1, TP.HCM", "0901234567");
        Store ch02 = ensureStore("CH02", "MiniMart — Cửa hàng 2 (Thủ Đức)", "45 Võ Văn Ngân, TP.Thủ Đức", "0902223344");
        seedUsers(ch01, ch02);
        seedStoreConfig(ch01, "MB Bank", "970422");
        seedStoreConfig(ch02, "Vietcombank", "970436");
        seedCustomers();
        seedPromotions();
        seedOpenShiftForCashier();
    }

    /** Chi nhánh: trả về dòng đã có (theo mã) hoặc tạo mới. */
    private Store ensureStore(String code, String name, String address, String phone) {
        return storeRepository.findByCode(code).orElseGet(() -> {
            Store s = new Store();
            s.setCode(code);
            s.setName(name);
            s.setAddress(address);
            s.setPhone(phone);
            s.setStatus(CommonStatus.ACTIVE);
            Store saved = storeRepository.save(s);
            log.info("Đã tạo chi nhánh {} — {}", code, name);
            return saved;
        });
    }

    /**
     * Tài khoản demo theo mô hình phân tầng:
     *  - 1 ADMIN quản trị TOÀN CHUỖI (không gắn cửa hàng).
     *  - Mỗi cửa hàng: 1 quản lý (MANAGER) + 1 nhân viên (STAFF). (Có thể tạo thêm N người tuỳ ý ở màn Tài khoản.)
     */
    private void seedUsers(Store ch01, Store ch02) {
        ensureUser("admin", "Quản trị viên (toàn chuỗi)", Role.ADMIN, null);
        ensureUser("manager", "Quản lý cửa hàng 1", Role.MANAGER, ch01);
        ensureUser("staff", "Nhân viên cửa hàng 1", Role.STAFF, ch01);
        ensureUser("manager2", "Quản lý cửa hàng 2", Role.MANAGER, ch02);
        ensureUser("staff2", "Nhân viên cửa hàng 2", Role.STAFF, ch02);
    }

    private void ensureUser(String username, String fullName, Role role, Store store) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setFullName(fullName);
            user.setRole(role);
            user.setStore(store);
            user.setStatus(UserStatus.ACTIVE);
            user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            userRepository.save(user);
            log.info("Đã tạo tài khoản demo '{}' (mật khẩu 123456)", username);
        } else {
            boolean dirty = false;
            if (!passwordEncoder.matches(DEMO_PASSWORD, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
                dirty = true;
            }
            // Đồng bộ vai trò + cửa hàng của tài khoản demo về đúng mô hình mới (idempotent, sửa cả DB cũ).
            if (user.getRole() != role) { user.setRole(role); dirty = true; }
            Long cur = user.getStore() != null ? user.getStore().getId() : null;
            Long want = store != null ? store.getId() : null;
            if (!java.util.Objects.equals(cur, want)) { user.setStore(store); dirty = true; }
            if (dirty) userRepository.save(user);
        }
    }

    private void seedStoreConfig(Store store, String bankName, String bankBin) {
        if (storeConfigRepository.findById(store.getId()).isPresent()) return;
        StoreConfig cfg = new StoreConfig();
        cfg.setId(store.getId());          // 1–1 chia sẻ khóa với stores.id
        cfg.setName(store.getName());
        cfg.setAddress(store.getAddress());
        cfg.setPhone(store.getPhone());
        cfg.setTaxCode("0312345678");
        cfg.setBankName(bankName);
        cfg.setBankBin(bankBin);
        cfg.setBankAccountNo("xxxxxxxxxxxx");
        cfg.setBankAccountName("CHU TAI KHOAN");
        cfg.setTransferPrefix("POS");
        cfg.setTelegramEnabled(false);
        cfg.setNotifyPayment(true);
        cfg.setNotifyLowStock(true);
        cfg.setNotifyNewInvoice(false);
        storeConfigRepository.save(cfg);
        log.info("Đã tạo cấu hình cho chi nhánh {}.", store.getCode());
    }

    private void seedCustomers() {
        // Khách thân thiết đa dạng (điểm tích lũy khác nhau) để minh hoạ CRM / báo cáo / chương trình điểm.
        ensureCustomer("Nguyễn Văn An", "0987654321", "an.nguyen@email.com", 50);
        ensureCustomer("Trần Thị Bình", "0978123456", null, 0);
        ensureCustomer("Lê Hoàng Cường", "0903456789", "cuong.le@email.com", 320);
        ensureCustomer("Phạm Thị Dung", "0912345678", "dung.pham@email.com", 145);
        ensureCustomer("Võ Minh Đức", "0934567890", null, 8);
        ensureCustomer("Đặng Thị En", "0945678901", "en.dang@email.com", 0);
        ensureCustomer("Bùi Quốc Phong", "0956789012", null, 210);
        ensureCustomer("Hoàng Thị Giang", "0967890123", "giang.hoang@email.com", 60);
        ensureCustomer("Đỗ Văn Hải", "0978901234", null, 25);
        ensureCustomer("Ngô Thị Hương", "0989012345", "huong.ngo@email.com", 480);
        ensureCustomer("Trịnh Văn Khoa", "0901112233", null, 95);
        ensureCustomer("Lý Thị Lan", "0902223344", "lan.ly@email.com", 12);
        ensureCustomer("Vương Minh Nam", "0903334455", null, 175);
        ensureCustomer("Châu Thị Oanh", "0904445566", "oanh.chau@email.com", 0);
        ensureCustomer("Tô Văn Phúc", "0905556677", null, 38);
        ensureCustomer("Mai Thị Quỳnh", "0906667788", "quynh.mai@email.com", 260);
    }

    private void ensureCustomer(String fullName, String phone, String email, int points) {
        Customer c = customerRepository.findByPhone(phone).orElseGet(() -> {
            Customer x = new Customer();
            x.setFullName(fullName);
            x.setPhone(phone);
            x.setEmail(email);
            x.setLoyaltyPoints(points);
            return customerRepository.save(x);
        });
        // Sổ cái điểm là NGUỒN SỰ THẬT: số dư ban đầu phải có một dòng sổ cái MỞ tương ứng để
        // Σ(sổ cái) == số dư (nếu không sẽ bị job đối soát cảnh báo "lệch sổ cái"). Idempotent:
        // chỉ ghi khi khách CÓ điểm mà CHƯA có dòng sổ cái nào → tự backfill cả CSDL demo cũ.
        if (points > 0 && ledgerRepository.countByCustomerId(c.getId()) == 0) {
            loyaltyService.record(c.getId(), null, points, "OPENING", points);
        }
    }

    private void seedPromotions() {
        LocalDateTime start = LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusYears(1).atStartOfDay();
        // Bộ khuyến mãi đa dạng: % và số tiền, nhiều mức đơn tối thiểu, có/không giới hạn lượt dùng.
        ensurePromotion("SALE10", "Giảm 10% đơn từ 100k", DiscountType.PERCENT,
                BigDecimal.valueOf(10), BigDecimal.valueOf(100000), start, end, 100);
        ensurePromotion("GIAM20K", "Giảm 20k đơn từ 200k", DiscountType.AMOUNT,
                BigDecimal.valueOf(20000), BigDecimal.valueOf(200000), start, end, null);
        ensurePromotion("WELCOME5", "Khách mới giảm 5% đơn từ 50k", DiscountType.PERCENT,
                BigDecimal.valueOf(5), BigDecimal.valueOf(50000), start, end, 500);
        ensurePromotion("CUOITUAN8", "Cuối tuần giảm 8% đơn từ 80k", DiscountType.PERCENT,
                BigDecimal.valueOf(8), BigDecimal.valueOf(80000), start, end, null);
        ensurePromotion("GIAM50K", "Giảm 50k đơn từ 500k", DiscountType.AMOUNT,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(500000), start, end, 200);
        ensurePromotion("SALE15", "Giảm 15% đơn từ 300k", DiscountType.PERCENT,
                BigDecimal.valueOf(15), BigDecimal.valueOf(300000), start, end, 50);
        ensurePromotion("FREESHIP15", "Hỗ trợ 15k đơn từ 150k", DiscountType.AMOUNT,
                BigDecimal.valueOf(15000), BigDecimal.valueOf(150000), start, end, null);
        ensurePromotion("VIP12", "Khách VIP giảm 12% đơn từ 250k", DiscountType.PERCENT,
                BigDecimal.valueOf(12), BigDecimal.valueOf(250000), start, end, 80);
    }

    private void ensurePromotion(String code, String name, DiscountType type, BigDecimal value,
                                 BigDecimal minOrder, LocalDateTime start, LocalDateTime end, Integer limit) {
        if (promotionRepository.existsByCode(code)) return;
        Promotion p = new Promotion();
        p.setCode(code);
        p.setName(name);
        p.setDiscountType(type);
        p.setDiscountValue(value);
        p.setMinOrderAmount(minOrder);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setUsageLimit(limit);
        p.setUsedCount(0);
        p.setStatus(CommonStatus.ACTIVE);
        promotionRepository.save(p);
    }

    /**
     * Mở sẵn 1 ca cho nhân viên cửa hàng 1 để vào POS bán được ngay — CHỈ trên CSDL hoàn toàn mới
     * (chưa có ca nào). Tránh việc mỗi lần khởi động lại sinh thêm 1 ca rỗng.
     */
    private void seedOpenShiftForCashier() {
        if (shiftRepository.count() > 0) return;
        userRepository.findByUsername("staff").ifPresent(staff -> {
            WorkShift shift = new WorkShift();
            shift.setStore(staff.getStore());
            shift.setUser(staff);
            shift.setOpeningCash(BigDecimal.valueOf(500000));
            shift.setStatus(ShiftStatus.OPEN);
            shiftRepository.save(shift);
            log.info("Đã mở sẵn 1 ca cho nhân viên cửa hàng 1 (tiền đầu ca 500.000đ).");
        });
    }
}
