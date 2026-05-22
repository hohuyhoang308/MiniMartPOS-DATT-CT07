package com.pos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.entity.PaymentTransaction;
import com.pos.entity.StoreConfig;
import com.pos.entity.enums.PaymentStatus;
import com.pos.exception.BadRequestException;
import com.pos.repository.PaymentTransactionRepository;
import com.pos.repository.StoreConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Đối soát thanh toán tự động qua WEB2M (FR-A4): poll lịch sử giao dịch ngân hàng,
 * khớp PENDING theo SỐ TIỀN + NỘI DUNG CK → cập nhật PAID + bank_reference, bắn Telegram.
 * VietQR chỉ hiển thị QR; WEB2M mới là cơ chế xác nhận tiền vào.
 */
@Service
public class Web2mSyncService {

    private static final Logger log = LoggerFactory.getLogger(Web2mSyncService.class);

    private final StoreConfigRepository storeConfigRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final TelegramService telegramService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public Web2mSyncService(StoreConfigRepository storeConfigRepository,
                            PaymentTransactionRepository paymentRepository,
                            TelegramService telegramService,
                            ObjectMapper objectMapper) {
        this.storeConfigRepository = storeConfigRepository;
        this.paymentRepository = paymentRepository;
        this.telegramService = telegramService;
        this.objectMapper = objectMapper;
    }

    /** Chạy đối soát một lần. Trả về số giao dịch khớp & xác nhận. */
    @Transactional
    public int sync() {
        StoreConfig cfg = storeConfigRepository.findById(StoreConfig.SINGLETON_ID).orElse(null);
        if (cfg == null || cfg.getWeb2mApiUrl() == null || cfg.getWeb2mApiUrl().isBlank()) {
            throw new BadRequestException("Chưa cấu hình URL API WEB2M");
        }
        List<PaymentTransaction> pending = paymentRepository.findByStatus(PaymentStatus.PENDING);
        if (pending.isEmpty()) return 0;

        JsonNode transactions = fetchTransactions(cfg.getWeb2mApiUrl());
        if (transactions == null || !transactions.isArray()) return 0;

        int matched = 0;
        for (PaymentTransaction pt : pending) {
            for (JsonNode tx : transactions) {
                if (matches(pt, tx)) {
                    pt.setStatus(PaymentStatus.PAID);
                    pt.setBankReference(extractReference(tx));
                    pt.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(pt);
                    matched++;
                    notifyPaid(pt);
                    break;
                }
            }
        }
        return matched;
    }

    /** Kiểm tra kết nối API WEB2M (FR-A6 nút "kiểm tra kết nối"). */
    public boolean testConnection(String apiUrl) {
        String url = (apiUrl != null && !apiUrl.isBlank()) ? apiUrl
                : storeConfigRepository.findById(StoreConfig.SINGLETON_ID)
                    .map(StoreConfig::getWeb2mApiUrl).orElse(null);
        if (url == null || url.isBlank()) {
            throw new BadRequestException("Chưa cấu hình URL API WEB2M");
        }
        return fetchTransactions(url) != null;
    }

    // ----- helpers -----

    private JsonNode fetchTransactions(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15)).GET().build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("WEB2M trả mã {}: {}", resp.statusCode(), resp.body());
                return null;
            }
            JsonNode root = objectMapper.readTree(resp.body());
            // WEB2M thường trả { "status": true, "transactions": [...] }
            JsonNode txs = root.get("transactions");
            return txs != null ? txs : root.get("data");
        } catch (Exception e) {
            log.error("Lỗi gọi WEB2M: {}", e.getMessage());
            return null;
        }
    }

    private boolean matches(PaymentTransaction pt, JsonNode tx) {
        // Số tiền khớp
        BigInteger expected = pt.getAmount().toBigInteger();
        BigInteger actual = parseAmount(firstText(tx, "amount", "amountIn", "creditAmount", "money"));
        if (actual == null || expected.compareTo(actual) != 0) return false;
        // Nội dung CK chứa transfer_content (đã chuẩn hóa)
        String desc = firstText(tx, "description", "content", "addInfo", "transactionContent", "comment");
        if (desc == null) return false;
        return normalize(desc).contains(normalize(pt.getTransferContent()));
    }

    private void notifyPaid(PaymentTransaction pt) {
        if (telegramService.notifyPaymentEnabled()) {
            telegramService.broadcast("✅ <b>Đã nhận thanh toán QR</b>\n"
                    + "Hóa đơn: " + pt.getInvoice().getCode() + "\n"
                    + "Số tiền: " + pt.getAmount().toBigInteger() + "đ\n"
                    + "Mã GD NH: " + (pt.getBankReference() != null ? pt.getBankReference() : "-"));
        }
    }

    private String extractReference(JsonNode tx) {
        String ref = firstText(tx, "transactionID", "tid", "refNo", "id", "transactionId");
        return ref != null ? ref : "WEB2M-" + System.identityHashCode(tx);
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String f : fields) {
            JsonNode v = node.get(f);
            if (v != null && !v.isNull()) return v.asText();
        }
        return null;
    }

    private static BigInteger parseAmount(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : new BigInteger(digits);
    }

    /** Chuẩn hóa nội dung: bỏ dấu cách & ký tự đặc biệt, in hoa — để khớp linh hoạt. */
    private static String normalize(String s) {
        return s.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
