package com.pos.repository;

import com.pos.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    /** 100 phiếu xuất hủy mới nhất của một CHI NHÁNH — màn lịch sử xuất hủy của MANAGER. */
    List<StockAdjustment> findTop100ByStore_IdOrderByIdDesc(Long storeId);
}
