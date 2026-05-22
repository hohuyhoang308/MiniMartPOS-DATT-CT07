package com.pos.repository.view;

import com.pos.entity.view.ShiftSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftSummaryViewRepository extends JpaRepository<ShiftSummaryView, Long> {

    Optional<ShiftSummaryView> findByShiftId(Long shiftId);
}
