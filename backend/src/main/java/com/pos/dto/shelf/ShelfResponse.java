package com.pos.dto.shelf;

import com.pos.entity.Shelf;
import com.pos.entity.enums.CommonStatus;

/** Kệ + sức chứa + thống kê đang chứa (số mặt hàng, tổng số lượng, còn trống). */
public record ShelfResponse(
        Long id, String code, String name, Integer capacity, CommonStatus status,
        int productCount, long totalQuantity, long freeSpace) {

    public static ShelfResponse from(Shelf s, int productCount, long totalQuantity) {
        int cap = s.getCapacity() != null ? s.getCapacity() : 0;
        long free = cap > 0 ? Math.max(0, cap - totalQuantity) : -1; // -1 = không giới hạn
        return new ShelfResponse(s.getId(), s.getCode(), s.getName(), cap, s.getStatus(),
                productCount, totalQuantity, free);
    }
}
