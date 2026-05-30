package com.pos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

/** Sinh & xác thực JWT (NFR4). Token chứa username (subject) + role + tên hiển thị. */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    /** Dấu hiệu secret mặc định (commit trong repo) — KHÔNG được dùng ở môi trường thật. */
    private static final String INSECURE_MARKER = "doi-secret-nay";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs,
                      Environment env) {
        boolean prod = Arrays.asList(env.getActiveProfiles()).contains("prod");
        boolean insecure = secret == null || secret.contains(INSECURE_MARKER);
        if (insecure) {
            if (prod) {
                // Ở production: KHÔNG cho chạy với secret mặc định → ngăn giả mạo token ADMIN.
                throw new IllegalStateException(
                        "JWT_SECRET chưa được đặt ở môi trường production. Hãy đặt biến môi trường JWT_SECRET (≥ 256-bit).");
            }
            log.warn("⚠ Đang dùng JWT secret MẶC ĐỊNH (commit trong repo) — chỉ chấp nhận khi DEV. "
                    + "Hãy đặt biến môi trường JWT_SECRET trước khi triển khai thật.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(CustomUserDetails user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole())
                .claim("fullName", user.getFullName())
                .claim("uid", user.getId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
