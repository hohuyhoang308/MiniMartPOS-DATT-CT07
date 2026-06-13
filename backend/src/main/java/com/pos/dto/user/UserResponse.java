package com.pos.dto.user;

import com.pos.entity.User;
import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id, String username, String fullName,
        Role role, Long storeId, String storeName,
        UserStatus status, LocalDateTime createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(),
                u.getRole(),
                u.getStore() != null ? u.getStore().getId() : null,
                u.getStore() != null ? u.getStore().getName() : null,
                u.getStatus(), u.getCreatedAt());
    }
}
