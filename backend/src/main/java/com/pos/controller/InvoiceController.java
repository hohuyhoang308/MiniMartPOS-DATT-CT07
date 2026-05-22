package com.pos.controller;

import com.pos.common.ApiResponse;
import com.pos.dto.invoice.InvoiceResponse;
import com.pos.dto.sale.CreateInvoiceRequest;
import com.pos.entity.enums.InvoiceStatus;
import com.pos.service.InvoicePdfService;
import com.pos.service.InvoiceService;
import com.pos.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Hóa đơn & bán hàng (FR4, FR5). Tạo HĐ: mọi vai trò có ca; hủy HĐ: Manager/Admin. */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final SaleService saleService;
    private final InvoiceService invoiceService;
    private final InvoicePdfService pdfService;

    public InvoiceController(SaleService saleService, InvoiceService invoiceService,
                             InvoicePdfService pdfService) {
        this.saleService = saleService;
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
    }

    /** Tạo hóa đơn (UC10) — transaction: trừ tồn FIFO, tích điểm, tăng lượt KM. */
    @PostMapping
    public ApiResponse<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest req) {
        return ApiResponse.ok("Tạo hóa đơn thành công", saleService.createInvoice(req));
    }

    @GetMapping
    public ApiResponse<List<InvoiceResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) InvoiceStatus status) {
        return ApiResponse.ok(invoiceService.search(date, customerId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(invoiceService.findById(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<InvoiceResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Đã hủy hóa đơn, tồn kho được hoàn tự động", invoiceService.cancel(id));
    }

    /** Xuất hóa đơn PDF khổ 80mm (UC12). */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] data = pdfService.export(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
