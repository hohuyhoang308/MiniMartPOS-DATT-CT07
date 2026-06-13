package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Cấu hình từng chi nhánh (FR10 + FR-A) — bảng {@code store_config}, MỖI chi nhánh MỘT dòng.
 *  Khóa chính {@code id} TRÙNG với {@code stores.id} (quan hệ 1–1 chia sẻ khóa) nên mỗi cửa hàng
 *  có ngân hàng/WEB2M/Telegram riêng. Hub: thông tin cửa hàng + ngân hàng (VietQR) + WEB2M + Telegram.
 *  LƯU Ý: web2m_api_url & telegram_bot_token là NHẠY CẢM. */
@Entity
@Table(name = "store_config")
@Getter
@Setter
@NoArgsConstructor
public class StoreConfig {

    /** Chi nhánh mặc định (dữ liệu cấu hình cũ trước đa chuỗi thuộc về chi nhánh này). */
    public static final long DEFAULT_STORE_ID = 1L;

    /** = stores.id (1–1 chia sẻ khóa). KHÔNG tự sinh: gán bằng id của chi nhánh. */
    @Id
    private Long id;

    // --- Thông tin cửa hàng (in hóa đơn - FR10) ---
    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(name = "tax_code", length = 30)
    private String taxCode;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    // --- Ngân hàng nhận tiền — sinh VietQR (FR-A1) ---
    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "bank_bin", length = 20)
    private String bankBin;

    @Column(name = "bank_account_no", length = 30)
    private String bankAccountNo;

    @Column(name = "bank_account_name", length = 100)
    private String bankAccountName;

    /** Mã ký hiệu trong nội dung CK (vd "POS"). */
    @Column(name = "transfer_prefix", length = 20)
    private String transferPrefix;

    // --- WEB2M — API poll đối soát (FR-A4) ---
    @Column(name = "web2m_api_url", length = 255)
    private String web2mApiUrl;

    // --- Telegram Bot — thông báo (FR-A5) ---
    @Column(name = "telegram_bot_token", length = 255)
    private String telegramBotToken;

    @Column(name = "telegram_enabled", nullable = false)
    private Boolean telegramEnabled = false;

    @Column(name = "notify_payment", nullable = false)
    private Boolean notifyPayment = true;

    @Column(name = "notify_low_stock", nullable = false)
    private Boolean notifyLowStock = true;

    @Column(name = "notify_new_invoice", nullable = false)
    private Boolean notifyNewInvoice = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
