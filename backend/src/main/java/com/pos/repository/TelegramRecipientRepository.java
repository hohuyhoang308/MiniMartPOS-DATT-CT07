package com.pos.repository;

import com.pos.entity.TelegramRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelegramRecipientRepository extends JpaRepository<TelegramRecipient, Long> {

    /** Người nhận đang bật của MỘT chi nhánh (config_id = stores.id) — để phát thông báo đúng chi nhánh. */
    List<TelegramRecipient> findByConfigIdAndIsActiveTrue(Long configId);

    /** Danh sách người nhận của một chi nhánh (quản lý ở màn cấu hình). */
    List<TelegramRecipient> findByConfigIdOrderById(Long configId);

    boolean existsByConfigIdAndChatId(Long configId, String chatId);
}
