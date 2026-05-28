package com.pos.repository;

import com.pos.entity.ShelfReturn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelfReturnRepository extends JpaRepository<ShelfReturn, Long> {

    long countByShelfId(Long shelfId);
}
