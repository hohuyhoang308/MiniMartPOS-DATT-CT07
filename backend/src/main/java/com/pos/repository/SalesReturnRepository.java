package com.pos.repository;

import com.pos.entity.SalesReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {

    List<SalesReturn> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    List<SalesReturn> findTop200ByOrderByCreatedAtDesc();
}
