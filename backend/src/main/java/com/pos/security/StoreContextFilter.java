package com.pos.security;

import com.pos.repository.StoreRepository;
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
 *
 * <p>Header được KIỂM TRA tồn tại trong bảng {@code stores}: id sai định dạng / không có thật → bỏ qua
 * (coi như chưa chọn = toàn chuỗi) thay vì để lỗi 500 phát sinh sâu trong service khi ghi dữ liệu.</p>
 */
@Component
public class StoreContextFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Store-Id";

    private final StoreRepository storeRepository;

    public StoreContextFilter(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String h = request.getHeader(HEADER);
            if (h != null && !h.isBlank()) {
                try {
                    long id = Long.parseLong(h.trim());
                    if (storeRepository.existsById(id)) {
                        StoreContext.setHeaderStoreId(id);
                    } // chi nhánh không tồn tại → bỏ qua (toàn chuỗi)
                } catch (NumberFormatException ignored) { /* header sai định dạng → coi như không chọn */ }
            }
            filterChain.doFilter(request, response);
        } finally {
            StoreContext.clear();
        }
    }
}
