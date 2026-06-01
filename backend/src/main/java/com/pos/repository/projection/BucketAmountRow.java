package com.pos.repository.projection;

import java.math.BigDecimal;

/** Một kỳ (bucket) + một số tiền — dùng để trừ hàng trả khỏi báo cáo theo kỳ. */
public interface BucketAmountRow {
    String getBucket();
    BigDecimal getAmount();
}
