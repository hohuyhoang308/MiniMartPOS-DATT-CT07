package com.pos.repository;

import com.pos.entity.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    boolean existsByPhone(String phone);

    List<Customer> findByPhoneContainingOrFullNameContainingIgnoreCase(String phone, String name);

    /**
     * Khóa GHI (SELECT … FOR UPDATE) hàng khách hàng để SERIALIZE việc đọc-rồi-ghi số dư điểm.
     * Customer là cấp CHUỖI (không gắn cửa hàng) nên 2 lần bán đồng thời cùng 1 khách — kể cả ở 2 chi nhánh —
     * nếu không khóa sẽ cùng đọc 1 số dư rồi cùng trừ → đổi/tích điểm gấp đôi (lost update).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);
}
