package com.pos.repository;

import com.pos.entity.EmployeePayProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeePayProfileRepository extends JpaRepository<EmployeePayProfile, Long> {

    Optional<EmployeePayProfile> findByUserId(Long userId);

    List<EmployeePayProfile> findByUserIdIn(List<Long> userIds);
}
