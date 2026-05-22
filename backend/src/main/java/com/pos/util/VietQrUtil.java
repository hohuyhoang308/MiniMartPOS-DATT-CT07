package com.pos.util;

import com.pos.entity.StoreConfig;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Dựng URL ảnh QR thanh toán theo chuẩn VietQR (FR-A1) — chỉ HIỂN THỊ, không xác nhận tiền.
 * Mẫu: https://img.vietqr.io/image/{BIN}-{accountNo}-compact2.png?amount=..&addInfo=..&accountName=..
 */
public final class VietQrUtil {

    private static final String BASE = "https://img.vietqr.io/image/";
    private static final String TEMPLATE = "compact2";

    private VietQrUtil() {}

    public static String buildQrUrl(StoreConfig cfg, BigDecimal amount, String transferContent) {
        if (cfg == null || cfg.getBankBin() == null || cfg.getBankAccountNo() == null) {
            return null; // chưa cấu hình ngân hàng
        }
        StringBuilder url = new StringBuilder(BASE)
                .append(cfg.getBankBin()).append('-')
                .append(cfg.getBankAccountNo()).append('-')
                .append(TEMPLATE).append(".png?amount=")
                .append(amount != null ? amount.toBigInteger().toString() : "0")
                .append("&addInfo=").append(encode(transferContent));
        if (cfg.getBankAccountName() != null) {
            url.append("&accountName=").append(encode(cfg.getBankAccountName()));
        }
        return url.toString();
    }

    private static String encode(String s) {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
