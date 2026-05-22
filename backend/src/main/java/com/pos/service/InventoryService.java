package com.pos.service;

import com.pos.dto.inventory.ExpiringBatchResponse;
import com.pos.dto.inventory.StockResponse;
import com.pos.repository.view.ExpiringBatchViewRepository;
import com.pos.repository.view.ProductStockViewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Tồn kho & cảnh báo (FR8 - UC17). Toàn bộ suy ra từ view (1 nguồn sự thật). */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final ProductStockViewRepository stockRepository;
    private final ExpiringBatchViewRepository expiringRepository;

    public InventoryService(ProductStockViewRepository stockRepository,
                            ExpiringBatchViewRepository expiringRepository) {
        this.stockRepository = stockRepository;
        this.expiringRepository = expiringRepository;
    }

    public List<StockResponse> currentStock() {
        return stockRepository.findAll().stream().map(StockResponse::from).toList();
    }

    public List<StockResponse> lowStock() {
        return stockRepository.findLowStock().stream().map(StockResponse::from).toList();
    }

    public List<ExpiringBatchResponse> expiringBatches() {
        return expiringRepository.findAllByOrderByDaysLeftAsc().stream()
                .map(ExpiringBatchResponse::from).toList();
    }
}
