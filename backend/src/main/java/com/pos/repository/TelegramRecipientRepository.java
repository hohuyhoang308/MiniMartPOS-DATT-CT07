package com.pos.repository;

import com.pos.entity.TelegramRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelegramRecipientRepository extends JpaRepository<TelegramRecipient, Long> {

    List<TelegramRecipient> findByIsActiveTrue();

    boolean existsByChatId(String chatId);
}
