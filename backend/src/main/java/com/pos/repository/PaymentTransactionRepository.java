package com.pos.repository;

import com.pos.entity.PaymentTransaction;
import com.pos.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByStatus(PaymentStatus status);

    Optional<PaymentTransaction> findFirstByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    Optional<PaymentTransaction> findByTransferContent(String transferContent);

    /** Các giao dịch QR còn PENDING nhưng đã quá hạn — để xử lý từng cái (EXPIRED + tự hủy HĐ). */
    List<PaymentTransaction> findByStatusAndExpiredAtBefore(PaymentStatus status, LocalDateTime time);
}
