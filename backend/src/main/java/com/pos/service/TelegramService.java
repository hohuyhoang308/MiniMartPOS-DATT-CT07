package com.pos.service;

import com.pos.entity.StoreConfig;
import com.pos.entity.TelegramRecipient;
import com.pos.exception.BadRequestException;
import com.pos.repository.StoreConfigRepository;
import com.pos.repository.TelegramRecipientRepository;
import com.pos.security.StoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** Gửi thông báo qua Telegram Bot THEO CHI NHÁNH (FR-A5, đa chuỗi). */
@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    private final StoreConfigRepository storeConfigRepository;
    private final TelegramRecipientRepository recipientRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public TelegramService(StoreConfigRepository storeConfigRepository,
                           TelegramRecipientRepository recipientRepository) {
        this.storeConfigRepository = storeConfigRepository;
        this.recipientRepository = recipientRepository;
    }

    /** Gửi 1 tin nhắn tới 1 chat. Trả true nếu Telegram nhận. */
    public boolean sendMessage(String botToken, String chatId, String text) {
        if (botToken == null || botToken.isBlank()) {
            throw new BadRequestException("Chưa cấu hình Telegram Bot Token");
        }
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage"
                    + "?chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                    + "&parse_mode=HTML"
                    + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            if (!ok) log.warn("Telegram gửi thất bại ({}): {}", resp.statusCode(), resp.body());
            return ok;
        } catch (Exception e) {
            log.error("Lỗi gửi Telegram: {}", e.getMessage());
            return false;
        }
    }

    /** Gửi tin thử (FR-A5) tới 1 chat bằng token của CỬA HÀNG được chọn (ADMIN truyền storeId). */
    public boolean testSend(Long storeId, String chatId, String text) {
        Long sid = StoreContext.resolveTargetStoreId(storeId);
        StoreConfig cfg = config(sid);
        return sendMessage(cfg.getTelegramBotToken(), chatId,
                text != null ? text : "🔔 Tin nhắn thử từ hệ thống POS cửa hàng tiện lợi");
    }

    /** Phát thông báo tới mọi người nhận đang bật của MỘT chi nhánh (nếu telegram_enabled). */
    public void broadcast(Long storeId, String text) {
        StoreConfig cfg = storeConfigRepository.findById(storeId).orElse(null);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getTelegramEnabled())
                || cfg.getTelegramBotToken() == null || cfg.getTelegramBotToken().isBlank()) {
            return;
        }
        List<TelegramRecipient> recipients = recipientRepository.findByConfigIdAndIsActiveTrue(storeId);
        for (TelegramRecipient r : recipients) {
            sendMessage(cfg.getTelegramBotToken(), r.getChatId(), text);
        }
    }

    /** Chi nhánh có bật thông báo "tiền về" không. */
    public boolean notifyPaymentEnabled(Long storeId) {
        StoreConfig cfg = storeConfigRepository.findById(storeId).orElse(null);
        return cfg != null && Boolean.TRUE.equals(cfg.getTelegramEnabled())
                && Boolean.TRUE.equals(cfg.getNotifyPayment());
    }

    private StoreConfig config(Long storeId) {
        return storeConfigRepository.findById(storeId)
                .orElseThrow(() -> new BadRequestException("Chưa có cấu hình cho chi nhánh"));
    }
}
