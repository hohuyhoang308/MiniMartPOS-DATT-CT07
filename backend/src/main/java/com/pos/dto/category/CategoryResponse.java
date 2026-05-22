package com.pos.dto.category;

import com.pos.entity.Category;
import com.pos.entity.enums.CommonStatus;

public record CategoryResponse(Long id, String name, String description, CommonStatus status) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getStatus());
    }
}
