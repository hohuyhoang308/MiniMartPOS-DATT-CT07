package com.pos.repository;

import com.pos.entity.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    Optional<PayrollPeriod> findByStoreIdAndPeriodMonth(Long storeId, String periodMonth);

    /** Kỳ lương của MỘT chi nhánh (đa chuỗi), mới nhất trước. */
    List<PayrollPeriod> findByStoreIdOrderByPeriodMonthDesc(Long storeId);

    /** Toàn chuỗi (ADMIN chưa chọn chi nhánh) — mới nhất trước. */
    List<PayrollPeriod> findTop200ByOrderByPeriodMonthDesc();
}
