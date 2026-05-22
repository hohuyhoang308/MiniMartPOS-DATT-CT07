package com.pos.dto.supplier;

import com.pos.entity.Supplier;
import com.pos.entity.enums.CommonStatus;

public record SupplierResponse(
        Long id, String name, String phone, String email, String address, CommonStatus status) {

    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getPhone(),
                s.getEmail(), s.getAddress(), s.getStatus());
    }
}
