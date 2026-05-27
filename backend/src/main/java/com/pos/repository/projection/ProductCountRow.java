package com.pos.repository.projection;

/** Sản phẩm + số lần xuất hiện (đếm) — dùng cho gợi ý "mua kèm" theo đồng xuất hiện. */
public interface ProductCountRow {
    Long getProductId();
    Long getCnt();
}
