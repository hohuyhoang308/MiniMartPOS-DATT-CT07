package com.pos.service;

import com.pos.dto.auth.LoginRequest;
import com.pos.dto.auth.LoginResponse;
import com.pos.dto.auth.MeResponse;
import com.pos.security.CustomUserDetails;
import com.pos.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Nghiệp vụ xác thực (FR1, UC01). */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        // Ném BadCredentialsException nếu sai mật khẩu, Disabled/LockedException nếu bị khóa
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole());
    }

    public MeResponse me(CustomUserDetails user) {
        return new MeResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
    }
}
