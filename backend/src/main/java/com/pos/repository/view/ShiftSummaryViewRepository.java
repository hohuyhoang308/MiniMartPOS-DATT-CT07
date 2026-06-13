package com.pos.repository.view;

import com.pos.entity.view.ShiftSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftSummaryViewRepository extends JpaRepository<ShiftSummaryView, Long> {

    Optional<ShiftSummaryView> findByShiftId(Long shiftId);

    /** Tổng hợp ca của MỘT chi nhánh (đa chuỗi) — báo cáo ca theo chi nhánh. */
    List<ShiftSummaryView> findByStoreId(Long storeId);
}
