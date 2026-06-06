package com.pos.service;

import com.pos.dto.product.ProductRequest;
import com.pos.dto.product.ProductResponse;
import com.pos.entity.Category;
import com.pos.entity.Product;
import com.pos.entity.Unit;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.view.ProductStockView;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.entity.Shelf;
import com.pos.repository.CategoryRepository;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.ShelfRepository;
import com.pos.repository.UnitRepository;
import com.pos.repository.projection.ProductCountRow;
import com.pos.repository.view.BatchStockViewRepository;
import com.pos.repository.view.ProductStockViewRepository;

import java.util.HashMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Quản lý sản phẩm (FR2.3, FR2.4 - UC05) + tra cứu mã vạch cho POS. */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final ProductStockViewRepository stockRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final BatchStockViewRepository batchStockRepository;
    private final ShelfRepository shelfRepository;
    private final AuditService auditService;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          UnitRepository unitRepository,
                          ProductStockViewRepository stockRepository,
                          InvoiceItemRepository invoiceItemRepository,
                          BatchStockViewRepository batchStockRepository,
                          ShelfRepository shelfRepository,
                          AuditService auditService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.stockRepository = stockRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.batchStockRepository = batchStockRepository;
        this.shelfRepository = shelfRepository;
        this.auditService = auditService;
    }

    /** Bản đồ sản phẩm → mã kệ đang bày (kệ có tồn &gt; 0) — để danh sách/POS biết hàng ở kệ nào. */
    private Map<Long, String> productShelfCode() {
        Map<Long, String> byId = shelfRepository.findAll().stream()
                .collect(Collectors.toMap(Shelf::getId, Shelf::getCode, (a, b) -> a));
        Map<Long, String> byProduct = new HashMap<>();
        for (var b : batchStockRepository.findAll()) {
            if (b.getShelfId() != null && b.getOnShelf() != null && b.getOnShelf() > 0) {
                byProduct.putIfAbsent(b.getProductId(), byId.get(b.getShelfId()));
            }
        }
        return byProduct;
    }

    /** Hỗ trợ tối thiểu: cần đồng xuất hiện ≥ ngần này HĐ mới coi là liên kết thật (lọc nhiễu đơn lẻ). */
    private static final long MIN_SUPPORT = 2;

    /**
     * Gợi ý "mua kèm" (market-basket): xếp theo LIFT thay vì đếm thô để KHÔNG bị hàng bán chạy
     * (túi nilon, nước suối…) lấn át. lift(A→B) = P(A∩B)/(P(A)·P(B)); với A cố định, xếp hạng tỉ lệ với
     * co(A,B)/n(B). Lọc theo {@link #MIN_SUPPORT}. Thiếu thì bù bằng sản phẩm CÙNG DANH MỤC. Chỉ món còn tồn kệ.
     */
    public List<ProductResponse> relatedProducts(Long productId, int limit) {
        Product base = getOrThrow(productId);
        Map<Long, ProductStockView> stock = stockRepository.findAll().stream()
                .collect(Collectors.toMap(ProductStockView::getProductId, v -> v, (a, b) -> a));

        // n(B): số HĐ chứa mỗi sản phẩm — mẫu số của lift
        Map<Long, Long> invoiceCount = invoiceItemRepository.invoiceCountByProduct().stream()
                .collect(Collectors.toMap(ProductCountRow::getProductId,
                        r -> r.getCnt() != null ? r.getCnt() : 1L, (a, b) -> a));

        LinkedHashMap<Long, Product> picked = new LinkedHashMap<>();
        // 1) Từ lịch sử: xếp theo lift = co(A,B)/n(B), lọc support tối thiểu, chỉ món còn hàng TRÊN KỆ
        invoiceItemRepository.boughtTogether(productId, PageRequest.of(0, Math.max(limit * 5, 20))).stream()
                .filter(r -> r.getCnt() != null && r.getCnt() >= MIN_SUPPORT)
                .sorted((a, b) -> Double.compare(
                        b.getCnt() / (double) Math.max(1L, invoiceCount.getOrDefault(b.getProductId(), 1L)),
                        a.getCnt() / (double) Math.max(1L, invoiceCount.getOrDefault(a.getProductId(), 1L))))
                .forEach(row -> {
                    if (picked.size() >= limit) return;
                    productRepository.findById(row.getProductId())
                            .filter(p -> p.getStatus() == CommonStatus.ACTIVE && shelfOf(stock.get(p.getId())) > 0)
                            .ifPresent(p -> picked.putIfAbsent(p.getId(), p));
                });
        // 2) Fallback: bù bằng sản phẩm cùng danh mục còn hàng trên kệ
        if (picked.size() < limit) {
            for (Product p : productRepository.search(null, base.getCategory().getId())) {
                if (picked.size() >= limit) break;
                if (p.getId().equals(productId) || p.getStatus() != CommonStatus.ACTIVE) continue;
                if (shelfOf(stock.get(p.getId())) > 0) picked.putIfAbsent(p.getId(), p);
            }
        }
        return picked.values().stream()
                .map(p -> ProductResponse.from(p, stock.get(p.getId())))
                .toList();
    }

    private static long shelfOf(ProductStockView v) {
        return v != null && v.getShelfStock() != null ? v.getShelfStock() : 0L;
    }

    /** Tìm/lọc sản phẩm; đính kèm tồn kho (tổng + kệ + kho) từ view (1 truy vấn gom). */
    public List<ProductResponse> search(String keyword, Long categoryId) {
        List<Product> products = productRepository.search(emptyToNull(keyword), categoryId);
        Map<Long, ProductStockView> stockMap = stockRepository.findAll().stream()
                .collect(Collectors.toMap(ProductStockView::getProductId, v -> v, (a, b) -> a));
        Map<Long, String> shelfByProduct = productShelfCode();
        return products.stream()
                .map(p -> ProductResponse.from(p, stockMap.get(p.getId()), shelfByProduct.get(p.getId())))
                .toList();
    }

    public ProductResponse findById(Long id) {
        Product p = getOrThrow(id);
        return ProductResponse.from(p, stockView(id));
    }

    /** Tra cứu nhanh theo mã vạch (POS, NFR1 < 1s). */
    public ProductResponse findByBarcode(String barcode) {
        Product p = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại (mã vạch: " + barcode + ")"));
        return ProductResponse.from(p, stockView(p.getId()));
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (productRepository.existsByBarcode(req.barcode())) {
            throw new BadRequestException("Mã vạch đã tồn tại: " + req.barcode());
        }
        Product p = new Product();
        apply(p, req);
        p.setStatus(req.status() != null ? req.status() : CommonStatus.ACTIVE);
        return ProductResponse.from(productRepository.save(p), 0L, 0L, 0L);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = getOrThrow(id);
        if (!p.getBarcode().equals(req.barcode()) && productRepository.existsByBarcode(req.barcode())) {
            throw new BadRequestException("Mã vạch đã tồn tại: " + req.barcode());
        }
        java.math.BigDecimal oldCost = p.getCostPrice(), oldSale = p.getSalePrice();
        apply(p, req);
        if (req.status() != null) p.setStatus(req.status());
        Product saved = productRepository.save(p);
        // Audit khi đổi GIÁ (vốn/bán) — minh bạch, chống lạm dụng đổi giá.
        if (oldCost == null || oldSale == null
                || oldCost.compareTo(saved.getCostPrice()) != 0 || oldSale.compareTo(saved.getSalePrice()) != 0) {
            auditService.log("CHANGE_PRICE", "PRODUCT", saved.getId(),
                    saved.getName() + ": giá vốn " + oldCost + "→" + saved.getCostPrice()
                            + ", giá bán " + oldSale + "→" + saved.getSalePrice());
        }
        return ProductResponse.from(saved, stockView(id));
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(getOrThrow(id));
    }

    // ----- helpers -----

    private void apply(Product p, ProductRequest req) {
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> NotFoundException.of("danh mục", req.categoryId()));
        Unit unit = unitRepository.findById(req.unitId())
                .orElseThrow(() -> NotFoundException.of("đơn vị tính", req.unitId()));
        p.setBarcode(req.barcode());
        p.setName(req.name());
        p.setCategory(category);
        p.setUnit(unit);
        p.setCostPrice(req.costPrice());
        p.setSalePrice(req.salePrice());
        p.setTaxRate(req.taxRate() != null ? req.taxRate() : new java.math.BigDecimal("8.00"));
        p.setPackSize(req.packSize() != null && req.packSize() >= 1 ? req.packSize() : 1);
        if (req.packUnitId() != null) {
            p.setPackUnit(unitRepository.findById(req.packUnitId())
                    .orElseThrow(() -> NotFoundException.of("đơn vị mua", req.packUnitId())));
        } else {
            p.setPackUnit(null);
        }
        p.setImageUrl(req.imageUrl());
        p.setMinStock(req.minStock());
    }

    private ProductStockView stockView(Long productId) {
        return stockRepository.findByProductId(productId).orElse(null);
    }

    private Product getOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> NotFoundException.of("sản phẩm", id));
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
