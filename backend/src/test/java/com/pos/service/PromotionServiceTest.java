package com.pos.service;

import com.pos.dto.promotion.ValidatePromotionResponse;
import com.pos.entity.Promotion;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.DiscountType;
import com.pos.exception.BadRequestException;
import com.pos.repository.PromotionRepository;
import com.pos.service.promotion.AmountDiscountStrategy;
import com.pos.service.promotion.DiscountStrategyFactory;
import com.pos.service.promotion.PercentDiscountStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Kiểm thử logic kiểm tra & tính giảm giá (FR7.2) — thuần nghiệp vụ, không cần CSDL. */
@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock PromotionRepository repository;
    @Mock AuditService auditService;
    PromotionService service;

    // Dựng service với FACTORY THẬT (strategy PERCENT/AMOUNT là logic thuần, không cần CSDL) để kiểm
    // đúng cách tính số tiền giảm — thay vì mock factory. auditService chỉ dùng ở luồng ghi (không đụng ở đây).
    @BeforeEach
    void setUp() {
        service = new PromotionService(repository, auditService,
                new DiscountStrategyFactory(List.of(new PercentDiscountStrategy(), new AmountDiscountStrategy())));
    }

    private Promotion base(DiscountType type, BigDecimal value, BigDecimal minOrder) {
        Promotion p = new Promotion();
        p.setId(1L);
        p.setCode("SALE");
        p.setName("Test");
        p.setDiscountType(type);
        p.setDiscountValue(value);
        p.setMinOrderAmount(minOrder);
        p.setStartDate(LocalDateTime.now().minusDays(1));
        p.setEndDate(LocalDateTime.now().plusDays(1));
        p.setUsedCount(0);
        p.setStatus(CommonStatus.ACTIVE);
        return p;
    }

    @Test
    void percent_discount_is_computed() {
        when(repository.findByCode("SALE"))
                .thenReturn(Optional.of(base(DiscountType.PERCENT, BigDecimal.TEN, BigDecimal.ZERO)));

        var result = service.validateForSale("SALE", new BigDecimal("100000"));

        assertThat(result.discountAmount()).isEqualByComparingTo("10000");
    }

    @Test
    void amount_discount_is_returned_as_is() {
        when(repository.findByCode("SALE"))
                .thenReturn(Optional.of(base(DiscountType.AMOUNT, new BigDecimal("20000"), BigDecimal.ZERO)));

        var result = service.validateForSale("SALE", new BigDecimal("100000"));

        assertThat(result.discountAmount()).isEqualByComparingTo("20000");
    }

    @Test
    void discount_is_capped_at_subtotal() {
        when(repository.findByCode("SALE"))
                .thenReturn(Optional.of(base(DiscountType.AMOUNT, new BigDecimal("200000"), BigDecimal.ZERO)));

        var result = service.validateForSale("SALE", new BigDecimal("100000"));

        assertThat(result.discountAmount()).isEqualByComparingTo("100000"); // không vượt tổng tạm tính
    }

    @Test
    void rejects_when_below_minimum_order() {
        when(repository.findByCode("SALE"))
                .thenReturn(Optional.of(base(DiscountType.PERCENT, BigDecimal.TEN, new BigDecimal("200000"))));

        assertThatThrownBy(() -> service.validateForSale("SALE", new BigDecimal("100000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tối thiểu");
    }

    @Test
    void rejects_when_code_not_found() {
        when(repository.findByCode("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateForSale("NONE", new BigDecimal("100000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    void rejects_when_inactive() {
        Promotion p = base(DiscountType.PERCENT, BigDecimal.TEN, BigDecimal.ZERO);
        p.setStatus(CommonStatus.INACTIVE);
        when(repository.findByCode("SALE")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.validateForSale("SALE", new BigDecimal("100000")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejects_when_out_of_date_window() {
        Promotion p = base(DiscountType.PERCENT, BigDecimal.TEN, BigDecimal.ZERO);
        p.setStartDate(LocalDateTime.now().plusDays(1));
        p.setEndDate(LocalDateTime.now().plusDays(2));
        when(repository.findByCode("SALE")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.validateForSale("SALE", new BigDecimal("100000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("hiệu lực");
    }

    @Test
    void rejects_when_usage_limit_reached() {
        Promotion p = base(DiscountType.PERCENT, BigDecimal.TEN, BigDecimal.ZERO);
        p.setUsageLimit(5);
        p.setUsedCount(5);
        when(repository.findByCode("SALE")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.validateForSale("SALE", new BigDecimal("100000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("hết lượt");
    }

    @Test
    void validate_endpoint_returns_final_amount() {
        when(repository.findByCode("SALE"))
                .thenReturn(Optional.of(base(DiscountType.PERCENT, BigDecimal.TEN, BigDecimal.ZERO)));

        ValidatePromotionResponse resp = service.validate("SALE", new BigDecimal("100000"));

        assertThat(resp.discountAmount()).isEqualByComparingTo("10000");
        assertThat(resp.finalAmount()).isEqualByComparingTo("90000");
    }
}
