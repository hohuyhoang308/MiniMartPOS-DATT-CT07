package com.pos.service.promotion;

import com.pos.entity.enums.DiscountType;
import com.pos.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FACTORY (Obj 3) chọn {@link DiscountStrategy} theo {@link DiscountType}.
 *
 * <p>Spring inject SẴN danh sách mọi strategy (mỗi loại 1 bean) → factory tự lập bản đồ type→strategy.
 * Thêm một loại giảm giá mới = thêm 1 lớp {@code @Component} implement {@link DiscountStrategy};
 * KHÔNG phải sửa factory hay service (Open/Closed Principle).</p>
 */
@Component
public class DiscountStrategyFactory {

    private final Map<DiscountType, DiscountStrategy> byType = new EnumMap<>(DiscountType.class);

    public DiscountStrategyFactory(List<DiscountStrategy> strategies) {
        for (DiscountStrategy s : strategies) byType.put(s.type(), s);
    }

    public DiscountStrategy forType(DiscountType type) {
        DiscountStrategy s = byType.get(type);
        if (s == null) throw new BadRequestException("Loại khuyến mãi chưa hỗ trợ: " + type);
        return s;
    }
}
