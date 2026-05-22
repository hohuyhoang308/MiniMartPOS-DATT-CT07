package com.pos.repository;

import com.pos.entity.PaymentTransaction;
import com.pos.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByStatus(PaymentStatus status);

    Optional<PaymentTransaction> findFirstByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    Optional<PaymentTransaction> findByTransferContent(String transferContent);
}
