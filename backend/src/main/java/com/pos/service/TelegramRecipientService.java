package com.pos.service;

import com.pos.dto.integration.TelegramRecipientRequest;
import com.pos.dto.integration.TelegramRecipientResponse;
import com.pos.entity.TelegramRecipient;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.TelegramRecipientRepository;
import com.pos.security.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý danh sách Chat ID nhận thông báo Telegram theo CỬA HÀNG (FR-A5, đa cửa hàng). */
@Service
@Transactional(readOnly = true)
public class TelegramRecipientService {

    private final TelegramRecipientRepository repository;

    public TelegramRecipientService(TelegramRecipientRepository repository) {
        this.repository = repository;
    }

    /** ADMIN chỉ định {@code storeId}; MANAGER/STAFF lấy cửa hàng của họ. */
    private Long resolve(Long storeId) {
        return StoreContext.resolveTargetStoreId(storeId);
    }

    public List<TelegramRecipientResponse> findAll(Long storeId) {
        return repository.findByConfigIdOrderById(resolve(storeId)).stream()
                .map(TelegramRecipientResponse::from).toList();
    }

    @Transactional
    public TelegramRecipientResponse add(Long storeId, TelegramRecipientRequest req) {
        Long sid = resolve(storeId);
        if (repository.existsByConfigIdAndChatId(sid, req.chatId())) {
            throw new BadRequestException("Chat ID đã tồn tại trong cửa hàng: " + req.chatId());
        }
        TelegramRecipient r = new TelegramRecipient();
        r.setConfigId(sid);
        r.setChatId(req.chatId());
        r.setLabel(req.label());
        r.setIsActive(true);
        return TelegramRecipientResponse.from(repository.save(r));
    }

    @Transactional
    public void delete(Long id) {
        TelegramRecipient r = repository.findById(id)
                .orElseThrow(() -> NotFoundException.of("người nhận Telegram", id));
        // Cô lập đa cửa hàng: configId chính là id cửa hàng — chặn xóa người nhận của cửa hàng khác
        // (ADMIN (toàn chuỗi) chưa chọn chi nhánh được bỏ qua).
        StoreContext.assertSameStore(r.getConfigId());
        repository.delete(r);
    }
}
