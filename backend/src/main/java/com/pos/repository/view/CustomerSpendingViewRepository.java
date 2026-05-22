package com.pos.repository.view;

import com.pos.entity.view.CustomerSpendingView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerSpendingViewRepository extends JpaRepository<CustomerSpendingView, Long> {

    Optional<CustomerSpendingView> findByCustomerId(Long customerId);
}
