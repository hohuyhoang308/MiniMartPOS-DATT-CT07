package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.user.CreateUserRequest;
import com.pos.dto.user.ResetPasswordRequest;
import com.pos.dto.user.UpdateUserRequest;
import com.pos.dto.user.UserResponse;
import com.pos.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quản lý tài khoản nhân viên (UC02).
 * <ul>
 *   <li>ADMIN toàn chuỗi: toàn quyền (mọi cửa hàng, mọi vai trò).</li>
 *   <li>MANAGER: chỉ quản lý STAFF trong CHÍNH cửa hàng mình — phạm vi do {@link UserService} chốt
 *       (ép vai trò STAFF + cửa hàng của manager, chặn thao tác lên tài khoản ngoài phạm vi).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ApiResponse.ok("Tạo tài khoản thành công", service.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return ApiResponse.ok("Cập nhật tài khoản thành công", service.update(id, req));
    }

    @PutMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        service.resetPassword(id, req);
        return ApiResponse.message("Đặt lại mật khẩu thành công");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> lock(@PathVariable Long id) {
        service.lock(id);
        return ApiResponse.message("Đã khóa tài khoản");
    }
}
