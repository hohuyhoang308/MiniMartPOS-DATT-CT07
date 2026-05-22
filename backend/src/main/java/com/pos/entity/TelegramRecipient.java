package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Người nhận thông báo Telegram (FR-A5) — bảng {@code telegram_recipients}.
 *  Tách bảng để giữ 1NF (mỗi Chat ID một dòng). */
@Entity
@Table(name = "telegram_recipients")
@Getter
@Setter
@NoArgsConstructor
public class TelegramRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_id", nullable = false)
    private Byte configId = StoreConfig.SINGLETON_ID;

    /** ID số hoặc @username kênh. */
    @Column(name = "chat_id", nullable = false, unique = true, length = 50)
    private String chatId;

    @Column(length = 100)
    private String label;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
