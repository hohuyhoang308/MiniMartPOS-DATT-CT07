package com.pos.util;

import com.pos.entity.StoreConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VietQrUtilTest {

    private StoreConfig config() {
        StoreConfig c = new StoreConfig();
        c.setBankBin("970422");
        c.setBankAccountNo("0123456789");
        c.setBankAccountName("CHU TAI KHOAN");
        return c;
    }

    @Test
    void builds_url_with_bank_amount_and_encoded_content() {
        String url = VietQrUtil.buildQrUrl(config(), new BigDecimal("10000"), "POS HD0001");

        assertThat(url).startsWith("https://img.vietqr.io/image/970422-0123456789-");
        assertThat(url).contains("amount=10000");
        assertThat(url).contains("addInfo=POS+HD0001"); // dấu cách được mã hóa
    }

    @Test
    void returns_null_when_bank_not_configured() {
        assertThat(VietQrUtil.buildQrUrl(null, BigDecimal.TEN, "x")).isNull();
        assertThat(VietQrUtil.buildQrUrl(new StoreConfig(), BigDecimal.TEN, "x")).isNull();
    }
}
