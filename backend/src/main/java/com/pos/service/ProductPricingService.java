package com.pos.service;

import com.pos.config.CacheConfig;
import com.pos.dto.pricing.StorePriceResponse;
import com.pos.entity.Product;
import com.pos.entity.ProductStorePrice;
import com.pos.entity.Store;
import com.pos.entity.enums.CommonStatus;
import com.pos.exception.NotFoundException;
import com.pos.repository.ProductRepository;
import com.pos.repository.ProductStorePriceRepository;
import com.pos.repository.StoreRepository;
import com.pos.repository.UserRepository;
import com.pos.security.SecurityUtils;
import com.pos.security.StoreContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * GIÁ BÁN HIỆU LỰC theo chi nhánh (Objective 1.3). Giữ mô hình giá TẬP TRUNG:
 * giá chuẩn ở {@link Product}, override theo chi nhánh ở {@link ProductStorePrice}.
 *
 * <p>Giá luôn resolve ở SERVER (không tin client) = COALESCE(override ACTIVE, giá chuẩn).
 * Việc đọc giá override được cache theo (productId, storeId); mọi lần ghi đều evict cache
 * tương ứng để POS không bao giờ thấy giá cũ (xem cache invalidation Objective 2).</p>
 */
@Service
@Transactional(readOnly = true)
public class ProductPricingService {

    private final ProductStorePriceRepository priceRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ProductPricingService(ProductStorePriceRepository priceRepository,
                                 ProductRepository productRepository,
                                 StoreRepository storeRepository,
                                 UserRepository userRepository,
                                 AuditService auditService) {
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Giá bán hiệu lực của 1 sản phẩm tại 1 chi nhánh. storeId null (ADMIN toàn chuỗi) → giá chuẩn.
     * Cache để giảm tải DB ở POS; key gồm cả product để evict chính xác khi đổi giá.
     */
    @Cacheable(cacheNames = CacheConfig.STORE_PRICE, key = "#product.id + ':' + (#storeId == null ? 'CHAIN' : #storeId)")
    public BigDecimal effectiveSalePrice(Product product, Long storeId) {
        if (storeId == null) return product.getSalePrice();
        return priceRepository.findActive(product.getId(), storeId, CommonStatus.ACTIVE)
                .map(ProductStorePrice::getSalePrice)
                .orElse(product.getSalePrice());
    }

    /**
     * Map productId → giá override ACTIVE tại 1 chi nhánh, để OVERLAY hàng loạt lên danh sách sản phẩm ở POS
     * (tránh N truy vấn). storeId null (ADMIN toàn chuỗi) → rỗng (mọi sản phẩm dùng giá chuẩn).
     */
    public java.util.Map<Long, BigDecimal> activeOverridesForStore(Long storeId) {
        if (storeId == null) return java.util.Map.of();
        return priceRepository.findByStoreId(storeId).stream()
                .filter(psp -> psp.getStatus() == CommonStatus.ACTIVE)
                .collect(java.util.stream.Collectors.toMap(
                        psp -> psp.getProduct().getId(), ProductStorePrice::getSalePrice, (a, b) -> a));
    }

    /** Danh sách override của chi nhánh đang xem (StoreContext); ADMIN phải chọn chi nhánh.
     *  Map sang DTO NGAY trong transaction để tránh LazyInitializationException khi controller đọc quan hệ lazy. */
    public List<StorePriceResponse> listForCurrentStore() {
        return priceRepository.findByStoreId(StoreContext.requireStoreId())
                .stream().map(StorePriceResponse::from).toList();
    }

    /**
     * Đặt/giật giá override cho 1 sản phẩm tại CHI NHÁNH ĐANG XEM. MANAGER chỉ đụng được cửa hàng mình
     * (StoreContext chốt store); ADMIN phải chọn chi nhánh trước. Upsert: đã có thì cập nhật, chưa có thì tạo.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.STORE_PRICE, key = "#productId + ':' + T(com.pos.security.StoreContext).requireStoreId()")
    public StorePriceResponse setStorePrice(Long productId, BigDecimal salePrice) {
        if (salePrice == null || salePrice.signum() < 0)
            throw new com.pos.exception.BadRequestException("Giá bán không hợp lệ");
        Long storeId = StoreContext.requireStoreId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> NotFoundException.of("sản phẩm", productId));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> NotFoundException.of("chi nhánh", storeId));
        ProductStorePrice psp = priceRepository.findByProductIdAndStoreId(productId, storeId)
                .orElseGet(ProductStorePrice::new);
        psp.setProduct(product);
        psp.setStore(store);
        psp.setSalePrice(salePrice);
        psp.setStatus(CommonStatus.ACTIVE);
        psp.setUpdatedBy(userRepository.getReferenceById(SecurityUtils.currentUserId()));
        ProductStorePrice saved = priceRepository.save(psp);
        auditService.log("SET_STORE_PRICE", "PRODUCT", productId,
                "Giá riêng CN#" + storeId + " = " + salePrice);
        // Map sang DTO ngay trong transaction (tránh LazyInitializationException ở controller).
        return StorePriceResponse.from(saved);
    }

    /** Bỏ override → quay về giá chuẩn của chuỗi. */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.STORE_PRICE, key = "#productId + ':' + T(com.pos.security.StoreContext).requireStoreId()")
    public void clearStorePrice(Long productId) {
        Long storeId = StoreContext.requireStoreId();
        priceRepository.findByProductIdAndStoreId(productId, storeId).ifPresent(psp -> {
            priceRepository.delete(psp);
            auditService.log("CLEAR_STORE_PRICE", "PRODUCT", productId, "Bỏ giá riêng CN#" + storeId);
        });
    }
}
