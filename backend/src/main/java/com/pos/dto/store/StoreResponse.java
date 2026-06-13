package com.pos.dto.store;

import com.pos.entity.Store;
import com.pos.entity.enums.CommonStatus;

import java.time.LocalDateTime;

/** Thông tin chi nhánh (đa chuỗi). */
public record StoreResponse(
        Long id, String code, String name, String address, String phone,
        CommonStatus status, LocalDateTime createdAt) {

    public static StoreResponse from(Store s) {
        return new StoreResponse(s.getId(), s.getCode(), s.getName(), s.getAddress(),
                s.getPhone(), s.getStatus(), s.getCreatedAt());
    }
}
