package com.pos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Nạp chi nhánh đang chọn từ header {@code X-Store-Id} vào {@link StoreContext} cho CHAIN_ADMIN
 * (đa chuỗi). Người dùng gắn chi nhánh sẽ bỏ qua header này khi resolve. Luôn dọn ThreadLocal cuối request.
 */
@Component
public class StoreContextFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Store-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String h = request.getHeader(HEADER);
            if (h != null && !h.isBlank()) {
                try {
                    StoreContext.setHeaderStoreId(Long.parseLong(h.trim()));
                } catch (NumberFormatException ignored) { /* header sai định dạng → coi như không chọn */ }
            }
            filterChain.doFilter(request, response);
        } finally {
            StoreContext.clear();
        }
    }
}
