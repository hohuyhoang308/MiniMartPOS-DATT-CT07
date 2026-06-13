package com.pos.repository;

import com.pos.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByCode(String code);

    boolean existsByCode(String code);
}
