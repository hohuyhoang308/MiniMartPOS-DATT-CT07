package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Người nhận thông báo Telegram (FR-A5) — bảng {@code telegram_recipients}.
 *  Tách bảng để giữ 1NF (mỗi Chat ID một dòng). Gắn theo CHI NHÁNH qua {@code config_id} (= stores.id):
 *  mỗi chi nhánh có danh sách người nhận riêng; một Chat ID có thể trùng giữa các chi nhánh. */
@Entity
@Table(name = "telegram_recipients",
       uniqueConstraints = @UniqueConstraint(name = "uq_tele_store_chat", columnNames = {"config_id", "chat_id"}))
@Getter
@Setter
@NoArgsConstructor
public class TelegramRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** = store_config.id = stores.id (chi nhánh sở hữu người nhận này). */
    @Column(name = "config_id", nullable = false)
    private Long configId = StoreConfig.DEFAULT_STORE_ID;

    /** ID số hoặc @username kênh. */
    @Column(name = "chat_id", nullable = false, length = 50)
    private String chatId;

    @Column(length = 100)
    private String label;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
