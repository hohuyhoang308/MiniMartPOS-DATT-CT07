package com.pos.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Họ tên không được để trống") @Size(max = 100) String fullName,
        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
        @Email(message = "Email không hợp lệ") @Size(max = 100) String email
) {}
