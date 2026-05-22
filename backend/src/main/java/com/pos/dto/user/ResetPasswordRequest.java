package com.pos.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Mật khẩu mới không được để trống") @Size(min = 6, max = 100) String newPassword
) {}
