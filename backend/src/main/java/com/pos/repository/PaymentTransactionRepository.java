package com.pos.repository;

import com.pos.entity.PaymentTransaction;
import com.pos.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByStatus(PaymentStatus status);

    Optional<PaymentTransaction> findFirstByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    Optional<PaymentTransaction> findByTransferContent(String transferContent);

    /** Chuyển các giao dịch QR còn PENDING nhưng đã quá hạn → EXPIRED (dọn rác định kỳ). */
    @Modifying
    @Query("""
            UPDATE PaymentTransaction p
            SET p.status = com.pos.entity.enums.PaymentStatus.EXPIRED
            WHERE p.status = com.pos.entity.enums.PaymentStatus.PENDING
              AND p.expiredAt IS NOT NULL AND p.expiredAt < :now
            """)
    int expireStalePending(@Param("now") LocalDateTime now);
}
