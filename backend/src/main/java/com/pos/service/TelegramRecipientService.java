package com.pos.service;

import com.pos.dto.integration.TelegramRecipientRequest;
import com.pos.dto.integration.TelegramRecipientResponse;
import com.pos.entity.TelegramRecipient;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.TelegramRecipientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý danh sách Chat ID nhận thông báo Telegram (FR-A5). */
@Service
@Transactional(readOnly = true)
public class TelegramRecipientService {

    private final TelegramRecipientRepository repository;

    public TelegramRecipientService(TelegramRecipientRepository repository) {
        this.repository = repository;
    }

    public List<TelegramRecipientResponse> findAll() {
        return repository.findAll().stream().map(TelegramRecipientResponse::from).toList();
    }

    @Transactional
    public TelegramRecipientResponse add(TelegramRecipientRequest req) {
        if (repository.existsByChatId(req.chatId())) {
            throw new BadRequestException("Chat ID đã tồn tại: " + req.chatId());
        }
        TelegramRecipient r = new TelegramRecipient();
        r.setChatId(req.chatId());
        r.setLabel(req.label());
        r.setIsActive(true);
        return TelegramRecipientResponse.from(repository.save(r));
    }

    @Transactional
    public void delete(Long id) {
        TelegramRecipient r = repository.findById(id)
                .orElseThrow(() -> NotFoundException.of("người nhận Telegram", id));
        repository.delete(r);
    }
}
