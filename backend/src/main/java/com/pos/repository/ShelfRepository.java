package com.pos.repository;

import com.pos.entity.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {

    /** Mã kệ duy nhất TRONG một chi nhánh (đa chuỗi). */
    boolean existsByStoreIdAndCodeIgnoreCase(Long storeId, String code);

    Optional<Shelf> findByStoreIdAndCodeIgnoreCase(Long storeId, String code);

    /** Kệ của một chi nhánh, sắp theo mã. */
    List<Shelf> findByStoreIdOrderByCode(Long storeId);
}
