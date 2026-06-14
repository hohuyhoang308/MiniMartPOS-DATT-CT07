package com.pos.entity;

import com.pos.entity.enums.CommonStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GIÁ BÁN RIÊNG theo CHI NHÁNH (đa cửa hàng) — bảng {@code product_store_prices}.
 *
 * <p>Mô hình giá vẫn là TẬP TRUNG: {@link Product#getSalePrice()} là giá chuẩn của cả chuỗi.
 * Mỗi (sản phẩm, chi nhánh) có thể có TỐI ĐA 1 dòng override ACTIVE; khi bán, giá hiệu lực =
 * COALESCE(override ACTIVE của chi nhánh, giá chuẩn) — luôn resolve ở SERVER, không nhận từ client.</p>
 */
@Entity
@Table(name = "product_store_prices",
        uniqueConstraints = @UniqueConstraint(name = "uq_psp", columnNames = {"product_id", "store_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ProductStorePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /** Giá bán riêng tại chi nhánh (đã gồm VAT, như giá chuẩn). */
    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CommonStatus status = CommonStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
