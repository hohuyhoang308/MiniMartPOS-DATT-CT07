package com.pos.config;

import com.pos.entity.User;
import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;
import com.pos.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * BOOTSTRAP TÀI KHOẢN ADMIN ĐẦU TIÊN cho môi trường PRODUCTION.
 *
 * <p>Ở profile {@code prod}, {@link BaseDataSeeder} (@Profile("!prod")) KHÔNG chạy → CSDL không có sẵn
 * tài khoản demo. Lớp này tạo MỘT admin toàn chuỗi từ biến môi trường {@code ADMIN_USERNAME}/{@code ADMIN_PASSWORD}
 * để chủ hệ thống đăng nhập lần đầu, rồi tự tạo thêm tài khoản khác trong app.</p>
 *
 * <p>An toàn: chỉ chạy ở {@code prod}; chỉ tạo khi CSDL CHƯA có người dùng nào (không bao giờ ghi đè/đổi mật khẩu
 * tài khoản hiện có); KHÔNG đặt mật khẩu mặc định — nếu thiếu {@code ADMIN_PASSWORD} hoặc quá ngắn (&lt;8), bỏ qua
 * và in cảnh báo rõ ràng để người vận hành đặt biến môi trường rồi khởi động lại.</p>
 */
@Component
@Profile("prod")
@Order(10)
public class AdminBootstrapInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String fullName;

    public AdminBootstrapInitializer(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${app.bootstrap.admin.username:admin}") String username,
                                     @Value("${app.bootstrap.admin.password:}") String password,
                                     @Value("${app.bootstrap.admin.full-name:Quản trị viên}") String fullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Đã có người dùng → không can thiệp (không tạo lại, không đổi mật khẩu).
        if (userRepository.count() > 0) return;

        if (password == null || password.isBlank() || password.length() < 8) {
            log.error("""
                    ╔══════════════════════════════════════════════════════════════════════════╗
                    ║  CHƯA TẠO ĐƯỢC TÀI KHOẢN ADMIN (prod): CSDL trống và ADMIN_PASSWORD chưa  ║
                    ║  đặt (hoặc < 8 ký tự). Hãy đặt biến môi trường rồi khởi động lại:          ║
                    ║      ADMIN_USERNAME=<tên đăng nhập>   ADMIN_PASSWORD=<mật khẩu ≥ 8 ký tự>  ║
                    ║  (Không tạo mật khẩu mặc định vì lý do bảo mật.)                           ║
                    ╚══════════════════════════════════════════════════════════════════════════╝""");
            return;
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setFullName(fullName);
        admin.setRole(Role.ADMIN);     // quản trị TOÀN CHUỖI
        admin.setStore(null);          // không gắn cửa hàng
        admin.setStatus(UserStatus.ACTIVE);
        admin.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(admin);
        log.info("Đã tạo tài khoản ADMIN đầu tiên '{}' (prod) từ biến môi trường. "
                + "Hãy đăng nhập và đổi mật khẩu/tạo thêm tài khoản trong ứng dụng.", username);
    }
}
