package com.pos.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * TẦNG CACHE cho dữ liệu ĐỌC-NHIỀU/GHI-ÍT (Obj 2): danh mục sản phẩm, khuyến mãi, giá theo chi nhánh.
 *
 * <p><b>Quyết định kiến trúc:</b> dùng cache ABSTRACTION của Spring (@Cacheable/@CacheEvict trên service)
 * thay vì gọi Redis trực tiếp → nghiệp vụ KHÔNG phụ thuộc nhà cung cấp cache, và môi trường dev chạy
 * được mà không cần dựng Redis. Mặc định {@link ConcurrentMapCacheManager} (in-memory); bật profile
 * {@code redis} để chuyển sang {@link RedisCacheManager} ở môi trường nhiều instance.</p>
 *
 * <p><b>Invalidation:</b> mọi đường ghi (đổi giá theo chi nhánh, sửa sản phẩm, sửa khuyến mãi) đều gắn
 * {@code @CacheEvict} để POS không bao giờ đọc dữ liệu cũ — xem các *Service tương ứng.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Tên các vùng cache dùng trong toàn hệ thống. */
    public static final String CATALOG = "productCatalog";
    public static final String PROMOTIONS = "promotions";
    public static final String STORE_PRICE = "storePrice";

    /** Mặc định (dev / single-instance): cache in-memory, không cần hạ tầng ngoài. */
    @Bean
    @Profile("!redis")
    public CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager(CATALOG, PROMOTIONS, STORE_PRICE);
    }

    /** Môi trường thật (nhiều instance): cache phân tán Redis, TTL 10 phút chống dữ liệu treo. */
    @Bean
    @Profile("redis")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
