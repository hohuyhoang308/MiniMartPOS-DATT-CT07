package com.pos.dto.customer;

import com.pos.entity.Customer;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id, String fullName, String phone, String email,
        Integer loyaltyPoints, LocalDateTime createdAt) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getFullName(), c.getPhone(),
                c.getEmail(), c.getLoyaltyPoints(), c.getCreatedAt());
    }
}
