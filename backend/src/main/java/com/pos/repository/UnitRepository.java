package com.pos.repository;

import com.pos.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    boolean existsByNameIgnoreCase(String name);
}
