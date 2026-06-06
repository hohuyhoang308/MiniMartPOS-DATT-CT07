package com.pos.dto.inventory;

import com.pos.entity.view.BatchStockView;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Chi tiết một LÔ của sản phẩm: HSD, tồn kho/kệ — để thấy rõ "lô nào, HSD bao nhiêu". */
public record BatchDetailResponse(
        Long batchId,
        Long shelfId,            // kệ mà lô đang nằm (null nếu chưa lên kệ)
        String shelfCode,        // mã kệ (vd K06) — để biết lô đang ở kệ nào
        LocalDate expiryDate,
        Integer daysLeft,        // số ngày còn tới HSD (âm = đã quá hạn; null = không HSD)
        Integer quantityIn,      // số lượng nhập của lô
        Long quantityRemaining,  // tổng còn (kho + kệ)
        Long onShelf,            // đang trên kệ
        Long inWarehouse         // đang trong kho
) {
    public static BatchDetailResponse from(BatchStockView v, String shelfCode) {
        Integer days = v.getExpiryDate() != null
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(), v.getExpiryDate()) : null;
        return new BatchDetailResponse(v.getBatchId(), v.getShelfId(), shelfCode, v.getExpiryDate(), days,
                v.getQuantityIn(), v.getQuantityRemaining(), v.getOnShelf(), v.getInWarehouse());
    }
}
