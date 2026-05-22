package com.pos.repository;

import com.pos.entity.InvoiceItemBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemBatchRepository extends JpaRepository<InvoiceItemBatch, Long> {
}
