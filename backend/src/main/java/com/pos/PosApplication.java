package com.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Điểm khởi động ứng dụng Backend POS Cửa hàng tiện lợi.
 *
 * <p>Kiến trúc phân lớp: Controller (REST) → Service → Repository → Entity, CSDL MySQL.
 * {@code @EnableScheduling} phục vụ job nền đối soát thanh toán WEB2M (FR-A4).
 */
@SpringBootApplication
@EnableScheduling
public class PosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PosApplication.class, args);
    }
}
