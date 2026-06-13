package com.pos.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pos.entity.Invoice;
import com.pos.entity.InvoiceItem;
import com.pos.entity.PaymentTransaction;
import com.pos.entity.StoreConfig;
import com.pos.exception.NotFoundException;
import com.pos.repository.InvoiceRepository;
import com.pos.repository.PaymentTransactionRepository;
import com.pos.repository.StoreConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/** Xuất hóa đơn PDF khổ 80mm (UC12, FR-A2). */
@Service
@Transactional(readOnly = true)
public class InvoicePdfService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY = new DecimalFormat("#,###");

    private final InvoiceRepository invoiceRepository;
    private final StoreConfigRepository storeConfigRepository;
    private final PaymentTransactionRepository paymentRepository;

    public InvoicePdfService(InvoiceRepository invoiceRepository,
                             StoreConfigRepository storeConfigRepository,
                             PaymentTransactionRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.storeConfigRepository = storeConfigRepository;
        this.paymentRepository = paymentRepository;
    }

    public byte[] export(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> NotFoundException.of("hóa đơn", invoiceId));
        StoreConfig cfg = storeConfigRepository.findById(inv.getStore().getId()).orElse(null);

        // Khổ 80mm (~226pt) chiều rộng, cao tự co
        Document doc = new Document(new Rectangle(226, 600), 12, 12, 12, 12);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bf = loadUnicodeFont();
            Font fTitle = new Font(bf, 12, Font.BOLD);
            Font fNormal = new Font(bf, 8, Font.NORMAL);
            Font fBold = new Font(bf, 8, Font.BOLD);

            String storeName = cfg != null ? cfg.getName() : "CỬA HÀNG TIỆN LỢI";
            addCenter(doc, storeName, fTitle);
            if (cfg != null && cfg.getAddress() != null) addCenter(doc, cfg.getAddress(), fNormal);
            if (cfg != null && cfg.getPhone() != null) addCenter(doc, "ĐT: " + cfg.getPhone(), fNormal);
            addCenter(doc, "HÓA ĐƠN BÁN HÀNG", fBold);
            if (cfg != null && cfg.getTaxCode() != null) addCenter(doc, "MST: " + cfg.getTaxCode(), fNormal);
            addLeft(doc, "Số HĐ: " + inv.getCode(), fNormal);
            addLeft(doc, "Ngày: " + inv.getCreatedAt().format(FMT), fNormal);
            addLeft(doc, "Thu ngân: " + inv.getShift().getUser().getFullName(), fNormal);
            if (inv.getCustomer() != null) {
                String kh = "Khách: " + inv.getCustomer().getFullName();
                if (inv.getCustomer().getPhone() != null) kh += " - " + inv.getCustomer().getPhone();
                addLeft(doc, kh, fNormal);
            }
            addLeft(doc, "--------------------------------", fNormal);

            // Bảng chi tiết: tên + (SL x đơn giá) + thành tiền
            PdfPTable table = new PdfPTable(new float[]{3.6f, 2.6f, 2.4f});
            table.setWidthPercentage(100);
            headerCell(table, "Sản phẩm", fBold);
            headerCell(table, "SL x ĐG", fBold);
            headerCell(table, "T.Tiền", fBold);
            for (InvoiceItem it : inv.getItems()) {
                bodyCell(table, it.getProduct().getName(), fNormal, Element.ALIGN_LEFT);
                bodyCell(table, it.getQuantity() + " x " + MONEY.format(it.getUnitPrice()), fNormal, Element.ALIGN_CENTER);
                bodyCell(table, MONEY.format(it.getSubtotal()), fNormal, Element.ALIGN_RIGHT);
            }
            doc.add(table);

            addLeft(doc, "--------------------------------", fNormal);
            addRight(doc, "Tạm tính: " + MONEY.format(inv.getSubtotal()) + "đ", fNormal);
            if (inv.getPromotion() != null) {
                addRight(doc, "Mã KM: " + inv.getPromotion().getCode(), fNormal);
            }
            if (inv.getPointsUsed() != null && inv.getPointsUsed() > 0) {
                addRight(doc, "Đổi điểm: " + inv.getPointsUsed() + " điểm", fNormal);
            }
            if (inv.getDiscountAmount() != null && inv.getDiscountAmount().signum() > 0) {
                addRight(doc, "Giảm trừ: -" + MONEY.format(inv.getDiscountAmount()) + "đ", fNormal);
            }
            addRight(doc, "TỔNG CỘNG: " + MONEY.format(inv.getTotalAmount()) + "đ", fBold);
            if (inv.getTaxAmount() != null && inv.getTaxAmount().signum() > 0) {
                addRight(doc, "(Trong đó thuế GTGT: " + MONEY.format(inv.getTaxAmount()) + "đ)", fNormal);
            }
            addLeft(doc, "Hình thức: " + (inv.getPaymentMethod().name().equals("CASH") ? "Tiền mặt" : "QR/Chuyển khoản"), fNormal);
            if (inv.getCustomerPaid() != null) {
                addRight(doc, "Tiền khách đưa: " + MONEY.format(inv.getCustomerPaid()) + "đ", fNormal);
                addRight(doc, "Tiền thừa: " + MONEY.format(inv.getChangeAmount()) + "đ", fNormal);
            }
            // Nội dung chuyển khoản (HĐ thanh toán QR)
            if (inv.getPaymentMethod().name().equals("QR")) {
                PaymentTransaction pt = paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(inv.getId()).orElse(null);
                if (pt != null) addLeft(doc, "Nội dung CK: " + pt.getTransferContent(), fNormal);
            }
            // Điểm thưởng khách thân thiết
            if (inv.getCustomer() != null) {
                String pts = "Điểm: ";
                if (inv.getPointsEarned() != null && inv.getPointsEarned() > 0) pts += "+" + inv.getPointsEarned() + " ";
                pts += "· Số dư: " + inv.getCustomer().getLoyaltyPoints();
                addLeft(doc, pts, fNormal);
            }
            addCenter(doc, " ", fNormal);
            addCenter(doc, "Cảm ơn quý khách & hẹn gặp lại!", fNormal);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Lỗi tạo PDF hóa đơn: " + e.getMessage(), e);
        }
    }

    /** Nạp font Unicode để hiển thị tiếng Việt (Arial trên Windows); fallback Helvetica. */
    private BaseFont loadUnicodeFont() {
        String[] candidates = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/segoeui.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        };
        for (String path : candidates) {
            try {
                if (new File(path).exists()) {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) { }
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("Không tải được font cho PDF", e);
        }
    }

    private void addCenter(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private void addLeft(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_LEFT);
        doc.add(p);
    }

    private void addRight(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        doc.add(p);
    }

    private void headerCell(PdfPTable t, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(Rectangle.BOTTOM);
        t.addCell(c);
    }

    private void bodyCell(PdfPTable t, String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
    }
}
