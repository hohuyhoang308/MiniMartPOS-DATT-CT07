package com.pos.repository;

import com.pos.entity.ProductStorePrice;
import com.pos.entity.enums.CommonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductStorePriceRepository extends JpaRepository<ProductStorePrice, Long> {

    Optional<ProductStorePrice> findByProductIdAndStoreId(Long productId, Long storeId);

    /** Giá override ĐANG hiệu lực của 1 sản phẩm tại 1 chi nhánh (dùng khi bán + cache). */
    @Query("""
            select psp from ProductStorePrice psp
            where psp.product.id = :productId and psp.store.id = :storeId and psp.status = :status
            """)
    Optional<ProductStorePrice> findActive(@Param("productId") Long productId,
                                           @Param("storeId") Long storeId,
                                           @Param("status") CommonStatus status);

    List<ProductStorePrice> findByStoreId(Long storeId);
}
