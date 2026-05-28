package com.pos.repository;

import com.pos.entity.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<Shelf> findByCodeIgnoreCase(String code);

    List<Shelf> findAllByOrderByCode();
}
