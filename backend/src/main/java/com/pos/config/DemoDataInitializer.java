package com.pos.config;

import com.pos.entity.User;
import com.pos.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Đảm bảo các tài khoản demo (admin/manager/cashier) đăng nhập được bằng mật khẩu "123456".
 * Lý do: hash mẫu trong schema.sql là BCrypt của "password" — nếu để nguyên sẽ không khớp tài liệu.
 * Chỉ chạy khi profile KHÔNG phải prod; chỉ sửa khi mật khẩu hiện tại chưa khớp "123456".
 */
@Component
@Profile("!prod")
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String DEMO_PASSWORD = "123456";
    private static final List<String> DEMO_USERS = List.of("admin", "manager", "cashier");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (String username : DEMO_USERS) {
            userRepository.findByUsername(username).ifPresent(this::ensurePassword);
        }
    }

    private void ensurePassword(User user) {
        if (!passwordEncoder.matches(DEMO_PASSWORD, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            userRepository.save(user);
            log.info("Đã đặt lại mật khẩu demo '{}' = \"123456\"", user.getUsername());
        }
    }
}
