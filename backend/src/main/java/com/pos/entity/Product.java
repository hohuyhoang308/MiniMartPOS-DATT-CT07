package com.pos.entity;

import com.pos.entity.enums.CommonStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Sản phẩm (FR2.3) — bảng {@code products}.
 *  KHÔNG có cột current_stock: tồn kho suy ra từ các lô (view v_product_stock). */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã vạch — khóa tra cứu POS (< 1s, NFR1). */
    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    /** Giá vốn hiện hành. */
    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    /** Mức tồn tối thiểu để cảnh báo (FR8.2). */
    @Column(name = "min_stock", nullable = false)
    private Integer minStock = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommonStatus status = CommonStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
