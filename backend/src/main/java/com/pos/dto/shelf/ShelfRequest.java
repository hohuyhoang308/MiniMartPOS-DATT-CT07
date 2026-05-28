package com.pos.dto.shelf;

import com.pos.entity.enums.CommonStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Tạo/sửa kệ. capacity = sức chứa tối đa (0 = không giới hạn). */
public record ShelfRequest(
        @NotBlank(message = "Mã kệ không được trống") String code,
        String name,
        @Min(value = 0, message = "Sức chứa phải ≥ 0") Integer capacity,
        CommonStatus status
) {}
