package com.pos.repository;

import com.pos.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    boolean existsByPhone(String phone);

    List<Customer> findByPhoneContainingOrFullNameContainingIgnoreCase(String phone, String name);
}
