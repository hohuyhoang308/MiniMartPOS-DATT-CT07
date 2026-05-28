package com.pos.repository;

import com.pos.entity.ShelfTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelfTransferRepository extends JpaRepository<ShelfTransfer, Long> {

    /** Số lần đã lên kệ tham chiếu tới một kệ (để chặn xoá kệ còn lịch sử). */
    long countByShelfId(Long shelfId);
}
