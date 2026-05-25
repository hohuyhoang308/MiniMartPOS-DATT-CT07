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

    public CustomerService(CustomerRepository repository,
                           InvoiceRepository invoiceRepository,
                           CustomerSpendingViewRepository spendingRepository) {
        this.repository = repository;
        this.invoiceRepository = invoiceRepository;
        this.spendingRepository = spendingRepository;
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
        return CustomerResponse.from(repository.save(c));
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
        return CustomerResponse.from(repository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    public CustomerHistoryResponse history(Long id) {
        Customer c = getOrThrow(id);
        var spending = spendingRepository.findByCustomerId(id);
        List<CustomerHistoryResponse.InvoiceBrief> invoices =
                invoiceRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(id, InvoiceStatus.COMPLETED)
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
