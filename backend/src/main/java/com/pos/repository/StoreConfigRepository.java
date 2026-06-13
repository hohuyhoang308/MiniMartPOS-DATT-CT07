package com.pos.repository;

import com.pos.entity.StoreConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/** Cấu hình theo chi nhánh — khóa = stores.id (Long). */
public interface StoreConfigRepository extends JpaRepository<StoreConfig, Long> {
}
