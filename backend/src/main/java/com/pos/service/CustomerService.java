package com.pos.service;

import com.pos.dto.customer.CustomerHistoryResponse;
import com.pos.dto.customer.CustomerRequest;
import com.pos.dto.customer.CustomerResponse;
import com.pos.entity.Customer;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.CustomerRepository;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.view.CustomerSpendingViewRepository;
import com.pos.security.StoreContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Quản lý khách hàng thân thiết (FR6 - UC15). */
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CustomerRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerSpendingViewRepository spendingRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository repository,
                           InvoiceRepository invoiceRepository,
                           CustomerSpendingViewRepository spendingRepository,
                           AuditService auditService) {
        this.repository = repository;
        this.invoiceRepository = invoiceRepository;
        this.spendingRepository = spendingRepository;
        this.auditService = auditService;
    }

    /** Tối đa số khách trả về khi không lọc (giới hạn payload khi dữ liệu lớn). */
    private static final int MAX_CUSTOMERS = 500;

    public List<CustomerResponse> search(String keyword) {
        List<Customer> list = (keyword == null || keyword.isBlank())
                ? repository.findAll(PageRequest.of(0, MAX_CUSTOMERS, Sort.by("fullName"))).getContent()
                : repository.findByPhoneContainingOrFullNameContainingIgnoreCase(keyword.trim(), keyword.trim());
        return list.stream().map(CustomerResponse::from).toList();
    }

    /** Tra khách theo SĐT (POS gắn khách nhanh). */
    public CustomerResponse findByPhone(String phone) {
        return repository.findByPhone(phone).map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng với SĐT: " + phone));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        if (repository.existsByPhone(req.phone())) {
            throw new BadRequestException("Số điện thoại đã tồn tại: " + req.phone());
        }
        Customer c = new Customer();
        c.setFullName(req.fullName());
        c.setPhone(req.phone());
        c.setEmail(req.email());
        c.setLoyaltyPoints(0);
        Customer saved = repository.save(c);
        auditService.log("CREATE_CUSTOMER", "CUSTOMER", saved.getId(),
                "Thêm khách hàng \"" + saved.getFullName() + "\" (SĐT " + saved.getPhone() + ")");
        return CustomerResponse.from(saved);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer c = getOrThrow(id);
        if (!c.getPhone().equals(req.phone()) && repository.existsByPhone(req.phone())) {
            throw new BadRequestException("Số điện thoại đã tồn tại: " + req.phone());
        }
        c.setFullName(req.fullName());
        c.setPhone(req.phone());
        c.setEmail(req.email());
        Customer saved = repository.save(c);
        auditService.log("UPDATE_CUSTOMER", "CUSTOMER", saved.getId(),
                "Sửa khách hàng \"" + saved.getFullName() + "\" (SĐT " + saved.getPhone() + ")");
        return CustomerResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Customer c = getOrThrow(id);
        repository.delete(c);
        auditService.log("DELETE_CUSTOMER", "CUSTOMER", c.getId(),
                "Xóa khách hàng \"" + c.getFullName() + "\" (SĐT " + c.getPhone() + ")");
    }

    public CustomerHistoryResponse history(Long id) {
        Customer c = getOrThrow(id);
        var spending = spendingRepository.findByCustomerId(id);
        // Cô lập đa cửa hàng: nhân viên/quản lý chỉ thấy lịch sử mua tại CHÍNH cửa hàng mình;
        // CHAIN_ADMIN (chưa chọn chi nhánh) thấy toàn chuỗi.
        List<CustomerHistoryResponse.InvoiceBrief> invoices =
                invoiceRepository.findCustomerHistory(id, InvoiceStatus.COMPLETED, StoreContext.currentStoreId())
                        .stream()
                        .map(i -> new CustomerHistoryResponse.InvoiceBrief(
                                i.getId(), i.getCode(), i.getTotalAmount(), i.getCreatedAt().format(FMT)))
                        .toList();
        return new CustomerHistoryResponse(
                c.getId(), c.getFullName(), c.getPhone(), c.getLoyaltyPoints(),
                spending.map(s -> s.getTotalSpent()).orElse(BigDecimal.ZERO),
                spending.map(s -> s.getInvoiceCount()).orElse(0L),
                invoices);
    }

    private Customer getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("khách hàng", id));
    }
}
