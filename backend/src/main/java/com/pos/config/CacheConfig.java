package com.pos.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TẦNG CACHE cho dữ liệu ĐỌC-NHIỀU/GHI-ÍT (Obj 2) — hiện áp dụng cho RESOLVE GIÁ THEO CHI NHÁNH
 * (giá bán hiệu lực của sản phẩm tại từng cửa hàng — xem {@code ProductPricingService}).
 *
 * <p><b>Quyết định kiến trúc:</b> dùng cache ABSTRACTION của Spring (@Cacheable/@CacheEvict trên service)
 * thay vì gọi một cache cụ thể → nghiệp vụ KHÔNG phụ thuộc nhà cung cấp cache. Triển khai hiện tại là
 * {@link ConcurrentMapCacheManager} (in-memory, đủ cho một instance); khi cần scale nhiều instance chỉ
 * việc thay bean này bằng cache phân tán (vd Redis) mà không sửa tầng nghiệp vụ — xem Hướng phát triển.</p>
 *
 * <p><b>Invalidation:</b> mọi đường ghi giá (đổi giá riêng chi nhánh, sửa giá chuẩn sản phẩm) đều gắn
 * {@code @CacheEvict} để giảm thiểu nguy cơ POS đọc giá cũ — xem các *Service tương ứng.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Tên các vùng cache dùng trong toàn hệ thống. */
    public static final String STORE_PRICE = "storePrice";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(STORE_PRICE);
    }
}
