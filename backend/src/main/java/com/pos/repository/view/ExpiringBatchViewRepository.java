package com.pos.repository.view;

import com.pos.entity.view.ExpiringBatchView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpiringBatchViewRepository extends JpaRepository<ExpiringBatchView, Long> {

    List<ExpiringBatchView> findAllByOrderByDaysLeftAsc();
}
