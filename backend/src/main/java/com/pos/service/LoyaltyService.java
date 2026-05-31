package com.pos.service;

import com.pos.entity.LoyaltyPointLedger;
import com.pos.repository.LoyaltyPointLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ghi sổ cái điểm tích lũy. Tham gia transaction của lời gọi (bán / hủy). */
@Service
public class LoyaltyService {

    private final LoyaltyPointLedgerRepository repository;

    public LoyaltyService(LoyaltyPointLedgerRepository repository) {
        this.repository = repository;
    }

    /** Ghi một dòng thay đổi điểm. {@code delta} +tích / −dùng; {@code balanceAfter} là số dư sau thay đổi. */
    @Transactional
    public void record(Long customerId, Long invoiceId, int delta, String reason, int balanceAfter) {
        if (customerId == null || delta == 0) return;
        LoyaltyPointLedger e = new LoyaltyPointLedger();
        e.setCustomerId(customerId);
        e.setInvoiceId(invoiceId);
        e.setDelta(delta);
        e.setReason(reason);
        e.setBalanceAfter(balanceAfter);
        repository.save(e);
    }
}
