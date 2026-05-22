package com.pos.repository;

import com.pos.entity.StoreConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreConfigRepository extends JpaRepository<StoreConfig, Byte> {
}
