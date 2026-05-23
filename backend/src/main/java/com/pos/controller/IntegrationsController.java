package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.integration.TelegramRecipientRequest;
import com.pos.dto.integration.TelegramRecipientResponse;
import com.pos.dto.integration.TestMessageRequest;
import com.pos.service.TelegramRecipientService;
import com.pos.service.TelegramService;
import com.pos.service.Web2mSyncService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Cấu hình tích hợp WEB2M & Telegram (FR-A6, UC24) — chỉ Admin. */
@RestController
@RequestMapping("/api/integrations")
@PreAuthorize("hasRole('ADMIN')")
public class IntegrationsController {

    private final TelegramRecipientService recipientService;
    private final TelegramService telegramService;
    private final Web2mSyncService web2mSyncService;

    public IntegrationsController(TelegramRecipientService recipientService,
                                  TelegramService telegramService,
                                  Web2mSyncService web2mSyncService) {
        this.recipientService = recipientService;
        this.telegramService = telegramService;
        this.web2mSyncService = web2mSyncService;
    }

    // --- Telegram recipients ---
    @GetMapping("/telegram/recipients")
    public ApiResponse<List<TelegramRecipientResponse>> recipients() {
        return ApiResponse.ok(recipientService.findAll());
    }

    @PostMapping("/telegram/recipients")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<TelegramRecipientResponse> addRecipient(@Valid @RequestBody TelegramRecipientRequest req) {
        return ApiResponse.ok("Thêm người nhận thành công", recipientService.add(req));
    }

    @DeleteMapping("/telegram/recipients/{id}")
    public ApiResponse<Void> deleteRecipient(@PathVariable Long id) {
        recipientService.delete(id);
        return ApiResponse.message("Đã xóa người nhận");
    }

    @PostMapping("/telegram/test")
    public ApiResponse<Map<String, Boolean>> testTelegram(@Valid @RequestBody TestMessageRequest req) {
        boolean ok = telegramService.testSend(req.chatId(), req.text());
        return ApiResponse.ok(ok ? "Đã gửi tin nhắn thử" : "Gửi thất bại — kiểm tra token/Chat ID",
                Map.of("sent", ok));
    }

    // --- WEB2M ---
    @PostMapping("/web2m/test")
    public ApiResponse<Map<String, Boolean>> testWeb2m(@RequestBody(required = false) Map<String, String> body) {
        String apiUrl = body != null ? body.get("apiUrl") : null;
        boolean ok = web2mSyncService.testConnection(apiUrl);
        return ApiResponse.ok(ok ? "Kết nối WEB2M thành công" : "Không kết nối được WEB2M",
                Map.of("connected", ok));
    }
}
