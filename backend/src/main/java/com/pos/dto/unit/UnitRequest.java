package com.pos.dto.unit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnitRequest(
        @NotBlank(message = "Tên đơn vị tính không được để trống")
        @Size(max = 50) String name
) {}
