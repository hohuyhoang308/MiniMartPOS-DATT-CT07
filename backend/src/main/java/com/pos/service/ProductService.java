package com.pos.service;

import com.pos.dto.product.ProductRequest;
import com.pos.dto.product.ProductResponse;
import com.pos.entity.Category;
import com.pos.entity.Product;
import com.pos.entity.Unit;
import com.pos.entity.enums.CommonStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.CategoryRepository;
import com.pos.repository.InvoiceItemRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.UnitRepository;
import com.pos.repository.view.ProductStockViewRepository;
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

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          UnitRepository unitRepository,
                          ProductStockViewRepository stockRepository,
                          InvoiceItemRepository invoiceItemRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.stockRepository = stockRepository;
        this.invoiceItemRepository = invoiceItemRepository;
    }

    /**
     * Gợi ý "mua kèm" (tiny-AI): ưu tiên sản phẩm hay được mua CHUNG hóa đơn (từ lịch sử bán);
     * nếu chưa đủ thì bù bằng sản phẩm CÙNG DANH MỤC (heuristic if/else). Chỉ trả hàng còn tồn.
     */
    public List<ProductResponse> relatedProducts(Long productId, int limit) {
        Product base = getOrThrow(productId);
        Map<Long, Long> stock = stockRepository.findAll().stream()
                .collect(Collectors.toMap(v -> v.getProductId(), v -> v.getCurrentStock(), (a, b) -> a));

        LinkedHashMap<Long, Product> picked = new LinkedHashMap<>();
        // 1) Từ lịch sử: sản phẩm hay mua chung hóa đơn
        for (var row : invoiceItemRepository.boughtTogether(productId, PageRequest.of(0, limit * 3))) {
            if (picked.size() >= limit) break;
            productRepository.findById(row.getProductId())
                    .filter(p -> p.getStatus() == CommonStatus.ACTIVE && stock.getOrDefault(p.getId(), 0L) > 0)
                    .ifPresent(p -> picked.putIfAbsent(p.getId(), p));
        }
        // 2) Fallback: bù bằng sản phẩm cùng danh mục còn hàng
        if (picked.size() < limit) {
            for (Product p : productRepository.search(null, base.getCategory().getId())) {
                if (picked.size() >= limit) break;
                if (p.getId().equals(productId) || p.getStatus() != CommonStatus.ACTIVE) continue;
                if (stock.getOrDefault(p.getId(), 0L) > 0) picked.putIfAbsent(p.getId(), p);
            }
        }
        return picked.values().stream()
                .map(p -> ProductResponse.from(p, stock.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    /** Tìm/lọc sản phẩm; đính kèm tồn kho hiện tại từ view (1 truy vấn gom). */
    public List<ProductResponse> search(String keyword, Long categoryId) {
        List<Product> products = productRepository.search(emptyToNull(keyword), categoryId);
        Map<Long, Long> stockMap = stockRepository.findAll().stream()
                .collect(Collectors.toMap(v -> v.getProductId(), v -> v.getCurrentStock(), (a, b) -> a));
        return products.stream()
                .map(p -> ProductResponse.from(p, stockMap.get(p.getId())))
                .toList();
    }

    public ProductResponse findById(Long id) {
        Product p = getOrThrow(id);
        return ProductResponse.from(p, currentStock(id));
    }

    /** Tra cứu nhanh theo mã vạch (POS, NFR1 < 1s). */
    public ProductResponse findByBarcode(String barcode) {
        Product p = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại (mã vạch: " + barcode + ")"));
        return ProductResponse.from(p, currentStock(p.getId()));
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (productRepository.existsByBarcode(req.barcode())) {
            throw new BadRequestException("Mã vạch đã tồn tại: " + req.barcode());
        }
        Product p = new Product();
        apply(p, req);
        p.setStatus(req.status() != null ? req.status() : CommonStatus.ACTIVE);
        return ProductResponse.from(productRepository.save(p), 0L);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = getOrThrow(id);
        if (!p.getBarcode().equals(req.barcode()) && productRepository.existsByBarcode(req.barcode())) {
            throw new BadRequestException("Mã vạch đã tồn tại: " + req.barcode());
        }
        apply(p, req);
        if (req.status() != null) p.setStatus(req.status());
        return ProductResponse.from(productRepository.save(p), currentStock(id));
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
        p.setImageUrl(req.imageUrl());
        p.setMinStock(req.minStock());
    }

    private Long currentStock(Long productId) {
        return stockRepository.findByProductId(productId)
                .map(v -> v.getCurrentStock()).orElse(0L);
    }

    private Product getOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> NotFoundException.of("sản phẩm", id));
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
