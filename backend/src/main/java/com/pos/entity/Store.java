package com.pos.entity;

import com.pos.entity.enums.CommonStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * CỬA HÀNG / CHI NHÁNH trong chuỗi — bảng {@code stores}.
 *
 * Nguyên tắc đa chuỗi của hệ thống:
 * <ul>
 *   <li>Gắn {@code store_id} TRỰC TIẾP vào: {@code users} (chi nhánh trực thuộc),
 *       {@code goods_receipts}, {@code work_shifts}, {@code shelves}, {@code invoices},
 *       {@code sales_returns} và {@code store_config} (1–1 theo chi nhánh).</li>
 *   <li>THỪA HƯỞNG chi nhánh, KHÔNG thêm cột: LÔ HÀNG ({@code goods_receipt_items}) qua phiếu nhập;
 *       dòng bán/phân bổ lô qua hóa đơn; điều chuyển/trả kệ qua kệ. Nhờ vậy tồn kho từng chi nhánh
 *       vẫn suy ra hoàn toàn từ chứng từ qua view {@code v_batch_stock}/{@code v_product_stock}
 *       (đã thêm chiều {@code store_id}).</li>
 *   <li>DÙNG CHUNG toàn chuỗi: danh mục sản phẩm, khách thân thiết, khuyến mãi, đơn vị, NCC,
 *       sổ điểm (một catalog, một chương trình điểm).</li>
 *   <li>Phân quyền: vai trò CHAIN_ADMIN quản trị toàn chuỗi (chọn chi nhánh qua header X-Store-Id);
 *       ADMIN/MANAGER/CASHIER gắn cứng một chi nhánh, mọi truy vấn tự lọc theo chi nhánh đó.</li>
 * </ul>
 */
@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
public class Store {

    /** Cửa hàng mặc định (dữ liệu cũ trước khi có đa chuỗi thuộc về cửa hàng này). */
    public static final long DEFAULT_ID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã cửa hàng duy nhất: CH01, CH02… */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommonStatus status = CommonStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
