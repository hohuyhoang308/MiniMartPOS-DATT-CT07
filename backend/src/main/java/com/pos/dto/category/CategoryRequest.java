package com.pos.dto.category;

import com.pos.entity.enums.CommonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 100) String name,
        @Size(max = 255) String description,
        CommonStatus status
) {}
