package com.pos.repository;

import com.pos.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Long> {
}
