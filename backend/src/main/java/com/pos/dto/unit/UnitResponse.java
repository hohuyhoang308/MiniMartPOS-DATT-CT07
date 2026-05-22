package com.pos.dto.unit;

import com.pos.entity.Unit;

public record UnitResponse(Long id, String name) {

    public static UnitResponse from(Unit u) {
        return new UnitResponse(u.getId(), u.getName());
    }
}
