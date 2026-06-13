package com.pos.service;

import com.pos.dto.auth.LoginRequest;
import com.pos.dto.auth.LoginResponse;
import com.pos.dto.auth.MeResponse;
import com.pos.exception.TooManyRequestsException;
import com.pos.security.CustomUserDetails;
import com.pos.security.JwtService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/** Nghiệp vụ xác thực (FR1, UC01). */
@Service
public class AuthService {

    /** Chống dò mật khẩu (brute-force): khóa tạm theo username sau nhiều lần sai. */
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 60_000; // khóa 60 giây

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /** username → [số lần sai liên tiếp, thời điểm hết khóa (epoch ms)]. */
    private final ConcurrentHashMap<String, long[]> attempts = new ConcurrentHashMap<>();

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        String key = request.username() != null ? request.username().toLowerCase() : "";
        long now = System.currentTimeMillis();
        long[] st = attempts.get(key);
        if (st != null && st[1] > now) {
            long secs = (st[1] - now) / 1000 + 1;
            throw new TooManyRequestsException("Đăng nhập sai quá nhiều lần. Thử lại sau " + secs + " giây.");
        }

        Authentication authentication;
        try {
            // Ném BadCredentialsException nếu sai mật khẩu, Disabled/LockedException nếu bị khóa
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            long[] cur = attempts.computeIfAbsent(key, k -> new long[]{0, 0});
            cur[0]++;
            if (cur[0] >= MAX_FAILS) { cur[1] = now + LOCK_MS; cur[0] = 0; } // khóa rồi reset đếm
            throw ex;
        }
        attempts.remove(key); // đăng nhập thành công → xóa bộ đếm

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getStoreId(),
                user.getStoreName());
    }

    public MeResponse me(CustomUserDetails user) {
        return new MeResponse(user.getId(), user.getUsername(), user.getFullName(),
                user.getRole(), user.getStoreId(), user.getStoreName());
    }
}
