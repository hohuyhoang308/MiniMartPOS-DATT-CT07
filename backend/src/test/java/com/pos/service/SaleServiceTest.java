package com.pos.service;

import com.pos.dto.invoice.InvoiceResponse;
import com.pos.dto.sale.CreateInvoiceRequest;
import com.pos.dto.sale.SaleItemRequest;
import com.pos.entity.*;
import com.pos.entity.enums.*;
import com.pos.entity.view.BatchStockView;
import com.pos.exception.BadRequestException;
import com.pos.exception.ConflictException;
import com.pos.repository.*;
import com.pos.repository.view.BatchStockViewRepository;
import com.pos.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử LÕI BÁN HÀNG (UC10/UC11, BR-01…BR-05): phân bổ lô FEFO (ưu tiên hạn gần),
 * thiếu tồn kệ → rollback, idempotency, kiểm tiền mặt, VAT trong giá, đổi/tích điểm,
 * lượt khuyến mãi hết do đơn chạy đồng thời. Thuần nghiệp vụ với repository mock.
 */
@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock ProductRepository productRepository;
    @Mock CustomerRepository customerRepository;
    @Mock WorkShiftRepository shiftRepository;
    @Mock BatchStockViewRepository batchStockRepository;
    @Mock GoodsReceiptItemRepository batchRepository;
    @Mock PaymentTransactionRepository paymentRepository;
    @Mock StoreConfigRepository storeConfigRepository;
    @Mock PromotionService promotionService;
    @Mock PromotionRepository promotionRepository;
    @Mock LoyaltyService loyaltyService;
    @Mock ProductPricingService pricingService;

    SaleService service;

    Store store;
    User cashier;
    WorkShift shift;
    Product product;

    @BeforeEach
    void setUp() {
        service = new SaleService(invoiceRepository, productRepository, customerRepository,
                shiftRepository, batchStockRepository, batchRepository, paymentRepository,
                storeConfigRepository, promotionService, promotionRepository, loyaltyService,
                pricingService);

        store = new Store();
        store.setId(1L);
        store.setStatus(CommonStatus.ACTIVE);

        cashier = new User();
        cashier.setId(10L);
        cashier.setUsername("staff");
        cashier.setFullName("Nhân viên A");
        cashier.setRole(Role.STAFF);
        cashier.setStatus(UserStatus.ACTIVE);
        cashier.setStore(store);

        shift = new WorkShift();
        shift.setId(20L);
        shift.setStore(store);
        shift.setUser(cashier);
        shift.setStatus(ShiftStatus.OPEN);

        product = new Product();
        product.setId(100L);
        product.setName("Nước suối 500ml");
        product.setTaxRate(new BigDecimal("8"));

        // Đăng nhập giả lập: STAFF gắn cửa hàng 1 → StoreContext lấy đúng cửa hàng này
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(cashier), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Lô khả dụng trên kệ (mock view v_batch_stock). */
    private BatchStockView batch(long batchId, long onShelf) {
        BatchStockView b = mock(BatchStockView.class);
        lenient().when(b.getBatchId()).thenReturn(batchId);
        lenient().when(b.getOnShelf()).thenReturn(onShelf);
        return b;
    }

    /** Stub luồng bán tiền mặt trơn tru: 1 sản phẩm giá 20.000đ, lô theo danh sách truyền vào.
     *  Dùng lenient() vì các test LUỒNG LỖI chủ đích dừng giữa chừng, không chạm tới mọi stub. */
    private void stubHappyPath(List<BatchStockView> batches) {
        lenient().when(shiftRepository.findFirstByUserIdAndStatusOrderByOpenedAtDesc(10L, ShiftStatus.OPEN))
                .thenReturn(Optional.of(shift));
        lenient().when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        lenient().when(pricingService.effectiveSalePrice(product, 1L)).thenReturn(new BigDecimal("20000"));
        lenient().when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(product));
        lenient().when(batchStockRepository.findAvailableBatchesFefo(100L, 1L)).thenReturn(batches);
        lenient().when(batchRepository.getReferenceById(anyLong())).thenAnswer(inv -> {
            GoodsReceiptItem g = new GoodsReceiptItem();
            g.setId(inv.getArgument(0));
            return g;
        });
        lenient().when(invoiceRepository.countByCodeStartingWith(any())).thenReturn(0L);
        lenient().when(invoiceRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(55L);
            return i;
        });
    }

    private CreateInvoiceRequest cashRequest(int quantity, String paid) {
        return new CreateInvoiceRequest(List.of(new SaleItemRequest(100L, quantity)),
                null, null, PaymentMethod.CASH, new BigDecimal(paid), null, null);
    }

    @Test
    void allocates_batches_in_fefo_order_across_two_batches() {
        // Lô 1 (hạn gần, view đã sắp trước) chỉ còn 2 trên kệ; lô 2 còn 5 — bán 3 phải rút 2 + 1
        stubHappyPath(List.of(batch(1L, 2L), batch(2L, 5L)));

        InvoiceResponse resp = service.createInvoice(cashRequest(3, "100000"));

        assertThat(resp.subtotal()).isEqualByComparingTo("60000");
        assertThat(resp.changeAmount()).isEqualByComparingTo("40000");

        var invoiceCaptor = org.mockito.ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).saveAndFlush(invoiceCaptor.capture());
        List<InvoiceItemBatch> allocs = invoiceCaptor.getValue().getItems().get(0).getBatches();
        assertThat(allocs).hasSize(2);
        assertThat(allocs.get(0).getBatch().getId()).isEqualTo(1L); // lô hạn gần bị rút TRƯỚC
        assertThat(allocs.get(0).getQuantity()).isEqualTo(2);
        assertThat(allocs.get(1).getBatch().getId()).isEqualTo(2L);
        assertThat(allocs.get(1).getQuantity()).isEqualTo(1);
    }

    @Test
    void computes_inclusive_vat_from_line_amount() {
        stubHappyPath(List.of(batch(1L, 10L)));

        InvoiceResponse resp = service.createInvoice(cashRequest(3, "100000"));

        // Giá đã gồm VAT: 60.000 × 8/108 = 4.444,44 → làm tròn về đồng khi chốt hóa đơn
        assertThat(resp.taxAmount()).isEqualByComparingTo("4444");
    }

    @Test
    void rejects_when_shelf_stock_insufficient_and_saves_nothing() {
        stubHappyPath(List.of(batch(1L, 2L))); // chỉ còn 2 trên kệ mà bán 3

        assertThatThrownBy(() -> service.createInvoice(cashRequest(3, "100000")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("không đủ hàng TRÊN KỆ");
        verify(invoiceRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejects_when_no_open_shift() {
        when(shiftRepository.findFirstByUserIdAndStatusOrderByOpenedAtDesc(10L, ShiftStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createInvoice(cashRequest(1, "100000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mở ca");
    }

    @Test
    void rejects_cash_paid_below_total() {
        stubHappyPath(List.of(batch(1L, 10L)));

        assertThatThrownBy(() -> service.createInvoice(cashRequest(3, "50000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("nhỏ hơn tổng tiền");
        verify(invoiceRepository, never()).saveAndFlush(any());
    }

    @Test
    void idempotent_retry_returns_existing_invoice_without_creating_new_one() {
        Invoice existing = new Invoice();
        existing.setId(99L);
        existing.setCode("HD20260724-0001");
        existing.setStore(store);
        existing.setShift(shift);
        when(invoiceRepository.findByIdempotencyKey("abc")).thenReturn(Optional.of(existing));
        when(paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(99L)).thenReturn(Optional.empty());

        CreateInvoiceRequest req = new CreateInvoiceRequest(List.of(new SaleItemRequest(100L, 1)),
                null, null, PaymentMethod.CASH, new BigDecimal("100000"), null, "abc");
        InvoiceResponse resp = service.createInvoice(req);

        assertThat(resp.code()).isEqualTo("HD20260724-0001");
        verify(invoiceRepository, never()).saveAndFlush(any()); // KHÔNG tạo hóa đơn thứ hai
    }

    @Test
    void redeems_points_capped_by_balance_and_writes_ledger() {
        stubHappyPath(List.of(batch(1L, 10L)));
        Customer customer = new Customer();
        customer.setId(7L);
        customer.setFullName("Khách quen");
        customer.setLoyaltyPoints(50);
        when(customerRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(customer));

        // Khách muốn dùng 999 điểm nhưng chỉ có 50 → dùng đúng 50 (50.000đ); còn phải trả 10.000đ → tích 1 điểm
        CreateInvoiceRequest req = new CreateInvoiceRequest(List.of(new SaleItemRequest(100L, 3)),
                7L, null, PaymentMethod.CASH, new BigDecimal("100000"), 999, null);
        InvoiceResponse resp = service.createInvoice(req);

        assertThat(resp.pointsUsed()).isEqualTo(50);
        assertThat(resp.discountAmount()).isEqualByComparingTo("50000");
        assertThat(resp.pointsEarned()).isEqualTo(1);
        assertThat(customer.getLoyaltyPoints()).isEqualTo(1); // 50 − 50 + 1
        verify(loyaltyService).record(7L, 55L, -50, "REDEEM", 0);
        verify(loyaltyService).record(7L, 55L, 1, "EARN", 1);
    }

    @Test
    void rolls_back_when_promotion_runs_out_of_uses_concurrently() {
        stubHappyPath(List.of(batch(1L, 10L)));
        Promotion promo = new Promotion();
        promo.setId(7L);
        promo.setCode("SALE10");
        when(promotionService.validateForSale("SALE10", new BigDecimal("60000")))
                .thenReturn(new PromotionService.ApplyResult(promo, new BigDecimal("10000")));
        when(promotionRepository.tryConsume(7L)).thenReturn(0); // đơn khác vừa dùng lượt cuối

        CreateInvoiceRequest req = new CreateInvoiceRequest(List.of(new SaleItemRequest(100L, 3)),
                null, "SALE10", PaymentMethod.CASH, new BigDecimal("100000"), null, null);

        assertThatThrownBy(() -> service.createInvoice(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("hết lượt");
    }

    @Test
    void qr_payment_creates_pending_invoice_with_unique_transfer_content() {
        stubHappyPath(List.of(batch(1L, 10L)));
        when(storeConfigRepository.findById(1L)).thenReturn(Optional.empty()); // chưa cấu hình → tiền tố mặc định POS
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateInvoiceRequest req = new CreateInvoiceRequest(List.of(new SaleItemRequest(100L, 3)),
                null, null, PaymentMethod.QR, null, null, null);
        InvoiceResponse resp = service.createInvoice(req);

        assertThat(resp.status()).isEqualTo(InvoiceStatus.PENDING_PAYMENT); // chưa tính doanh thu
        assertThat(resp.payment()).isNotNull();
        assertThat(resp.payment().transferContent()).isEqualTo("POS " + resp.code());
        assertThat(resp.payment().amount()).isEqualByComparingTo("60000");
    }
}
