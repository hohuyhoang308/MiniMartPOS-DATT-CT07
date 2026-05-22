package com.pos.repository;

import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    /** Ca đang mở của một thu ngân (mỗi thu ngân chỉ nên có 1 ca OPEN). */
    Optional<WorkShift> findFirstByUserIdAndStatusOrderByOpenedAtDesc(Long userId, ShiftStatus status);

    boolean existsByUserIdAndStatus(Long userId, ShiftStatus status);
}
