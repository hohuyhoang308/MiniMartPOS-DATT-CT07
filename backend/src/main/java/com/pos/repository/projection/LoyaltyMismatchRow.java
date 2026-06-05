package com.pos.repository.projection;

/** Một khách có số dư điểm (customers.loyalty_points) LỆCH với tổng sổ cái (Σ delta) — để đối soát. */
public interface LoyaltyMismatchRow {
    Long getCustomerId();
    String getCustomerName();
    Integer getStored();   // số dư đang lưu trên customer
    Long getLedger();      // tổng delta trong sổ cái
}
