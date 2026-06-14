package com.pos.service;

import com.pos.dto.promotion.PromotionRequest;
import com.pos.dto.promotion.PromotionResponse;
import com.pos.dto.promotion.ValidatePromotionResponse;
import com.pos.entity.Promotion;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.DiscountType;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.PromotionRepository;
import com.pos.service.promotion.DiscountStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Quản lý khuyến mãi (FR7 - UC16) + kiểm tra/áp dụng mã (UC11). */
@Service
@Transactional(readOnly = true)
public class PromotionService {

    private final PromotionRepository repository;
    private final AuditService auditService;
    private final DiscountStrategyFactory discountStrategyFactory;

    public PromotionService(PromotionRepository repository, AuditService auditService,
                            DiscountStrategyFactory discountStrategyFactory) {
        this.repository = repository;
        this.auditService = auditService;
        this.discountStrategyFactory = discountStrategyFactory;
    }

    /** Kết quả áp mã dùng nội bộ cho SaleService (entity + số tiền giảm). */
    public record ApplyResult(Promotion promotion, BigDecimal discountAmount) {}

    public List<PromotionResponse> findAll() {
        return repository.findAll().stream().map(PromotionResponse::from).toList();
    }

    @Transactional
    public PromotionResponse create(PromotionRequest req) {
        if (repository.existsByCode(req.code())) {
            throw new BadRequestException("Mã khuyến mãi đã tồn tại: " + req.code());
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        Promotion p = new Promotion();
        apply(p, req);
        p.setUsedCount(0);
        p.setStatus(req.status() != null ? req.status() : CommonStatus.ACTIVE);
        Promotion saved = repository.save(p);
        auditService.log("CREATE_PROMOTION", "PROMOTION", saved.getId(),
                "Thêm khuyến mãi " + saved.getCode() + " \"" + saved.getName() + "\" ("
                        + describeDiscount(saved) + ", đơn tối thiểu "
                        + saved.getMinOrderAmount().toPlainString() + "đ)");
        return PromotionResponse.from(saved);
    }

    @Transactional
    public PromotionResponse update(Long id, PromotionRequest req) {
        Promotion p = getOrThrow(id);
        if (!p.getCode().equals(req.code()) && repository.existsByCode(req.code())) {
            throw new BadRequestException("Mã khuyến mãi đã tồn tại: " + req.code());
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        apply(p, req);
        if (req.status() != null) p.setStatus(req.status());
        Promotion saved = repository.save(p);
        auditService.log("UPDATE_PROMOTION", "PROMOTION", saved.getId(),
                "Sửa khuyến mãi " + saved.getCode() + " \"" + saved.getName() + "\" ("
                        + describeDiscount(saved) + ", đơn tối thiểu "
                        + saved.getMinOrderAmount().toPlainString() + "đ)");
        return PromotionResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Promotion p = getOrThrow(id);
        repository.delete(p);
        auditService.log("DELETE_PROMOTION", "PROMOTION", p.getId(),
                "Xóa khuyến mãi " + p.getCode() + " \"" + p.getName() + "\"");
    }

    /** Diễn đạt mức giảm cho dễ hiểu: "giảm 10%" hoặc "giảm 20000đ". */
    private String describeDiscount(Promotion p) {
        if (p.getDiscountType() == DiscountType.PERCENT) {
            return "giảm " + p.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
        }
        return "giảm " + p.getDiscountValue().toPlainString() + "đ";
    }

    /** Endpoint kiểm tra mã khi thu ngân áp dụng (UC11). */
    public ValidatePromotionResponse validate(String code, BigDecimal subtotal) {
        ApplyResult result = validateForSale(code, subtotal);
        Promotion p = result.promotion();
        return new ValidatePromotionResponse(
                p.getId(), p.getCode(), p.getName(),
                result.discountAmount(), subtotal.subtract(result.discountAmount()));
    }

    /**
     * Kiểm tra hợp lệ & tính số tiền giảm (FR7.2). Ném BadRequest nếu không hợp lệ.
     * Dùng cả ở endpoint /validate lẫn trong giao dịch bán hàng.
     */
    public ApplyResult validateForSale(String code, BigDecimal subtotal) {
        Promotion p = repository.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại: " + code));

        if (p.getStatus() != CommonStatus.ACTIVE) {
            throw new BadRequestException("Mã giảm giá đã ngừng áp dụng");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(p.getStartDate()) || now.isAfter(p.getEndDate())) {
            throw new BadRequestException("Mã giảm giá không trong thời gian hiệu lực");
        }
        if (p.getUsageLimit() != null && p.getUsedCount() >= p.getUsageLimit()) {
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");
        }
        if (subtotal.compareTo(p.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Đơn hàng chưa đạt tối thiểu "
                    + p.getMinOrderAmount().toPlainString() + " để áp mã");
        }

        // STRATEGY (Obj 3): cách tính giảm tùy loại được tách thành các DiscountStrategy riêng.
        BigDecimal discount = discountStrategyFactory.forType(p.getDiscountType()).computeDiscount(p, subtotal);
        // Bất biến total ≥ 0: không cho giảm vượt quá tổng tạm tính (giữ một chỗ kiểm soát duy nhất).
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        return new ApplyResult(p, discount);
    }

    private void apply(Promotion p, PromotionRequest req) {
        p.setCode(req.code());
        p.setName(req.name());
        p.setDiscountType(req.discountType());
        p.setDiscountValue(req.discountValue());
        p.setMinOrderAmount(req.minOrderAmount() != null ? req.minOrderAmount() : BigDecimal.ZERO);
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        p.setUsageLimit(req.usageLimit());
    }

    private Promotion getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("khuyến mãi", id));
    }
}
