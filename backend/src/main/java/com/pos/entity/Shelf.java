package com.pos.entity;

import com.pos.entity.enums.CommonStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Kệ trưng bày vật lý trong cửa hàng (Kệ A1, Kệ 1...) — bảng {@code shelves}. */
@Entity
@Table(name = "shelves")
@Getter
@Setter
@NoArgsConstructor
public class Shelf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã kệ (duy nhất): A1, B2, K01... */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /** Tên/khu vực kệ (vd "Nước giải khát"). */
    @Column(length = 100)
    private String name;

    /** Sức chứa tối đa (tổng số sản phẩm). 0 = không giới hạn. */
    @Column(nullable = false)
    private Integer capacity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommonStatus status = CommonStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
