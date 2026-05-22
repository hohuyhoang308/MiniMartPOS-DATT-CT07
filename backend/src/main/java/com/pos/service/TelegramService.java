package com.pos.service;

import com.pos.entity.StoreConfig;
import com.pos.entity.TelegramRecipient;
import com.pos.exception.BadRequestException;
import com.pos.repository.StoreConfigRepository;
import com.pos.repository.TelegramRecipientRepository;
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

/** Gửi thông báo qua Telegram Bot (FR-A5). */
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

    /** Gửi tin thử (FR-A5 nút "gửi thử") tới 1 chat cụ thể bằng token đã cấu hình. */
    public boolean testSend(String chatId, String text) {
        StoreConfig cfg = config();
        return sendMessage(cfg.getTelegramBotToken(), chatId,
                text != null ? text : "🔔 Tin nhắn thử từ hệ thống POS cửa hàng tiện lợi");
    }

    /** Phát thông báo tới mọi người nhận đang bật (nếu telegram_enabled). */
    public void broadcast(String text) {
        StoreConfig cfg = storeConfigRepository.findById(StoreConfig.SINGLETON_ID).orElse(null);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getTelegramEnabled())
                || cfg.getTelegramBotToken() == null || cfg.getTelegramBotToken().isBlank()) {
            return;
        }
        List<TelegramRecipient> recipients = recipientRepository.findByIsActiveTrue();
        for (TelegramRecipient r : recipients) {
            sendMessage(cfg.getTelegramBotToken(), r.getChatId(), text);
        }
    }

    public boolean notifyPaymentEnabled() {
        StoreConfig cfg = storeConfigRepository.findById(StoreConfig.SINGLETON_ID).orElse(null);
        return cfg != null && Boolean.TRUE.equals(cfg.getTelegramEnabled())
                && Boolean.TRUE.equals(cfg.getNotifyPayment());
    }

    private StoreConfig config() {
        return storeConfigRepository.findById(StoreConfig.SINGLETON_ID)
                .orElseThrow(() -> new BadRequestException("Chưa có cấu hình cửa hàng"));
    }
}
