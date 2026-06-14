package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.auth.LoginRequest;
import com.pos.dto.auth.LoginResponse;
import com.pos.dto.auth.MeResponse;
import com.pos.security.CustomUserDetails;
import com.pos.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Xác thực (FR1). /login công khai; /me, /logout cần token. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest http) {
        return ApiResponse.ok("Đăng nhập thành công", authService.login(request, clientIp(http)));
    }

    /** IP thật của client: ưu tiên X-Forwarded-For (sau reverse proxy), nếu không có thì remote addr. */
    private static String clientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return http.getRemoteAddr();
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok(authService.me(user));
    }

    /** JWT stateless: đăng xuất do client xóa token; endpoint chỉ để FE gọi cho nhất quán. */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.message("Đăng xuất thành công (client xóa token)");
    }
}
