package com.pos.repository;

import com.pos.entity.StockTransfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    /** Phiếu liên quan chi nhánh đang xem (là nguồn HOẶC đích) — mới nhất trước. */
    @Query("""
            select t from StockTransfer t
            where t.sourceStore.id = :storeId or t.destStore.id = :storeId
            order by t.id desc
            """)
    List<StockTransfer> findForStore(@Param("storeId") Long storeId);

    List<StockTransfer> findAllByOrderByIdDesc();

    /** Khóa GHI phiếu khi đổi trạng thái để chặn 2 lần ship/receive đồng thời. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from StockTransfer t where t.id = :id")
    Optional<StockTransfer> findByIdForUpdate(@Param("id") Long id);

    long countByCodeStartingWith(String prefix);
}
