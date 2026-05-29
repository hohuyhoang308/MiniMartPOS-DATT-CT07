package com.pos.repository;

import com.pos.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    /**
     * Khoá ghi (PESSIMISTIC_WRITE) bản ghi sản phẩm để tuần tự hoá mọi thao tác đụng tồn của
     * sản phẩm đó (bán / lên kệ / lấy về kho). Tránh đua điều kiện làm tồn kệ/kho âm.
     * Quy ước: khi cần khoá nhiều sản phẩm phải khoá theo id TĂNG DẦN để tránh deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    /** Tìm/lọc sản phẩm theo từ khóa (tên/mã vạch) và danh mục (FR2.4). */
    @Query("""
            SELECT p FROM Product p
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR p.barcode LIKE CONCAT('%', :keyword, '%'))
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
            ORDER BY p.name
            """)
    List<Product> search(@Param("keyword") String keyword,
                         @Param("categoryId") Long categoryId);
}
