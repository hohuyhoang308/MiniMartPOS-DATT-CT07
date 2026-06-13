package com.pos.repository.view;

import com.pos.entity.view.ExpiringBatchView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpiringBatchViewRepository extends JpaRepository<ExpiringBatchView, Long> {

    List<ExpiringBatchView> findAllByOrderByDaysLeftAsc();

    /** Lô cận/quá HSD của MỘT chi nhánh (đa chuỗi). */
    List<ExpiringBatchView> findByStoreIdOrderByDaysLeftAsc(Long storeId);

    /** Số lô cận/quá HSD của 1 chi nhánh — KPI dashboard. */
    long countByStoreId(Long storeId);
}
