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
 * tài khoản đăng nhập, cấu hình cửa hàng, vài khách & khuyến mãi demo, và 1 ca mở sẵn cho thu ngân.
 *
 * <p>Idempotent (chỉ thêm phần còn thiếu) → an toàn chạy lại mỗi lần khởi động. Chỉ chạy khi
 * profile KHÁC prod. Chạy TRƯỚC {@link CatalogDemoDataInitializer} (cần có user để gán phiếu nhập).</p>
 */
@Component
@Profile("!prod")
@Order(10)
public class BaseDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BaseDataSeeder.class);
    private static final String DEMO_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final StoreConfigRepository storeConfigRepository;
    private final CustomerRepository customerRepository;
    private final PromotionRepository promotionRepository;
    private final WorkShiftRepository shiftRepository;
    private final PasswordEncoder passwordEncoder;

    public BaseDataSeeder(UserRepository userRepository,
                          StoreConfigRepository storeConfigRepository,
                          CustomerRepository customerRepository,
                          PromotionRepository promotionRepository,
                          WorkShiftRepository shiftRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.storeConfigRepository = storeConfigRepository;
        this.customerRepository = customerRepository;
        this.promotionRepository = promotionRepository;
        this.shiftRepository = shiftRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedStoreConfig();
        seedCustomers();
        seedPromotions();
        seedOpenShiftForCashier();
    }

    /** Tạo 3 tài khoản demo nếu thiếu; nếu đã có thì đảm bảo mật khẩu = "123456". */
    private void seedUsers() {
        ensureUser("admin", "Chủ cửa hàng", Role.ADMIN);
        ensureUser("manager", "Quản lý ca", Role.MANAGER);
        ensureUser("cashier", "Thu ngân A", Role.CASHIER);
    }

    private void ensureUser(String username, String fullName, Role role) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setFullName(fullName);
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            userRepository.save(user);
            log.info("Đã tạo tài khoản demo '{}' (mật khẩu 123456)", username);
        } else if (!passwordEncoder.matches(DEMO_PASSWORD, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            userRepository.save(user);
            log.info("Đã đặt lại mật khẩu demo '{}' = 123456", username);
        }
    }

    private void seedStoreConfig() {
        if (storeConfigRepository.findById(StoreConfig.SINGLETON_ID).isPresent()) return;
        StoreConfig cfg = new StoreConfig();
        cfg.setId(StoreConfig.SINGLETON_ID);
        cfg.setName("Cửa hàng tiện lợi MiniMart");
        cfg.setAddress("123 Đường Lê Lợi, Q.1, TP.HCM");
        cfg.setPhone("0901234567");
        cfg.setTaxCode("0312345678");
        cfg.setBankName("MB Bank");
        cfg.setBankBin("970422");
        cfg.setBankAccountNo("xxxxxxxxxxxx");
        cfg.setBankAccountName("CHU TAI KHOAN");
        cfg.setTransferPrefix("POS");
        cfg.setTelegramEnabled(false);
        cfg.setNotifyPayment(true);
        cfg.setNotifyLowStock(true);
        cfg.setNotifyNewInvoice(false);
        storeConfigRepository.save(cfg);
        log.info("Đã tạo cấu hình cửa hàng mặc định.");
    }

    private void seedCustomers() {
        ensureCustomer("Nguyễn Văn An", "0987654321", "an.nguyen@email.com", 50);
        ensureCustomer("Trần Thị Bình", "0978123456", null, 0);
    }

    private void ensureCustomer(String fullName, String phone, String email, int points) {
        if (customerRepository.existsByPhone(phone)) return;
        Customer c = new Customer();
        c.setFullName(fullName);
        c.setPhone(phone);
        c.setEmail(email);
        c.setLoyaltyPoints(points);
        customerRepository.save(c);
    }

    private void seedPromotions() {
        LocalDateTime start = LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusYears(1).atStartOfDay();
        ensurePromotion("SALE10", "Giảm 10% đơn từ 100k", DiscountType.PERCENT,
                BigDecimal.valueOf(10), BigDecimal.valueOf(100000), start, end, 100);
        ensurePromotion("GIAM20K", "Giảm 20k đơn từ 200k", DiscountType.AMOUNT,
                BigDecimal.valueOf(20000), BigDecimal.valueOf(200000), start, end, null);
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
     * Mở sẵn 1 ca cho thu ngân để vào POS bán được ngay — CHỈ trên CSDL hoàn toàn mới
     * (chưa có ca nào). Tránh việc mỗi lần khởi động lại sinh thêm 1 ca rỗng khi thu ngân
     * đang không có ca mở → tích lũy dữ liệu rác về lâu dài.
     */
    private void seedOpenShiftForCashier() {
        if (shiftRepository.count() > 0) return;
        userRepository.findByUsername("cashier").ifPresent(cashier -> {
            WorkShift shift = new WorkShift();
            shift.setUser(cashier);
            shift.setOpeningCash(BigDecimal.valueOf(500000));
            shift.setStatus(ShiftStatus.OPEN);
            shiftRepository.save(shift);
            log.info("Đã mở sẵn 1 ca cho thu ngân (tiền đầu ca 500.000đ).");
        });
    }
}
