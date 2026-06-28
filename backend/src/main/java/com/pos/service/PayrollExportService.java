package com.pos.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pos.entity.Payslip;
import com.pos.entity.PayrollPeriod;
import com.pos.entity.PayslipAdjustment;
import com.pos.entity.StoreConfig;
import com.pos.entity.enums.PayType;
import com.pos.entity.enums.PayrollStatus;
import com.pos.entity.enums.PayslipAdjustmentType;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.security.CustomUserDetails;
import com.pos.security.SecurityUtils;
import com.pos.repository.PayrollPeriodRepository;
import com.pos.repository.PayslipAdjustmentRepository;
import com.pos.repository.PayslipRepository;
import com.pos.repository.StoreConfigRepository;
import com.pos.security.StoreContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

/** Xuất bảng lương ra Excel (cả kỳ) và phiếu lương ra PDF (cá nhân) — module Lương. */
@Service
@Transactional(readOnly = true)
public class PayrollExportService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,###");

    private final PayrollPeriodRepository periodRepository;
    private final PayslipRepository payslipRepository;
    private final PayslipAdjustmentRepository adjustmentRepository;
    private final StoreConfigRepository storeConfigRepository;

    public PayrollExportService(PayrollPeriodRepository periodRepository,
                                PayslipRepository payslipRepository,
                                PayslipAdjustmentRepository adjustmentRepository,
                                StoreConfigRepository storeConfigRepository) {
        this.periodRepository = periodRepository;
        this.payslipRepository = payslipRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.storeConfigRepository = storeConfigRepository;
    }

    // =====================================================================
    //  EXCEL — BẢNG LƯƠNG CẢ KỲ
    // =====================================================================
    public byte[] exportPeriodExcel(Long periodId) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> NotFoundException.of("kỳ lương", periodId));
        StoreContext.assertSameStore(period.getStore().getId());   // chống IDOR đa chi nhánh
        List<Payslip> slips = payslipRepository.findByPeriodIdOrderByUserId(periodId);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bảng lương " + period.getPeriodMonth());

            CellStyle bold = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font bf = wb.createFont();
            bf.setBold(true);
            bold.setFont(bf);

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("BẢNG LƯƠNG THÁNG " + period.getPeriodMonth()
                    + " — " + period.getStore().getName());
            title.getCell(0).setCellStyle(bold);

            String[] cols = {"STT", "Nhân viên", "Loại lương", "Đơn giá (đ)", "Số ca",
                    "Giờ công", "Giờ thường", "Giờ tăng ca", "Lương gốc (đ)", "Tăng ca (đ)",
                    "Phụ cấp (đ)", "Lương gộp (đ)", "Thưởng (đ)", "Phạt (đ)", "Thực lĩnh (đ)"};
            Row header = sheet.createRow(2);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(bold);
            }

            int r = 3, stt = 1;
            for (Payslip p : slips) {
                Row row = sheet.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(stt++);
                row.createCell(c++).setCellValue(p.getUser().getFullName());
                row.createCell(c++).setCellValue(p.getPayType() == PayType.HOURLY ? "Theo giờ" : "Theo tháng");
                row.createCell(c++).setCellValue(d(p.getBaseRate()));
                row.createCell(c++).setCellValue(p.getShiftCount());
                row.createCell(c++).setCellValue(d(p.getWorkedHours()));
                row.createCell(c++).setCellValue(d(p.getRegularHours()));
                row.createCell(c++).setCellValue(d(p.getOtHours()));
                row.createCell(c++).setCellValue(d(p.getRegularPay()));
                row.createCell(c++).setCellValue(d(p.getOtPay()));
                row.createCell(c++).setCellValue(d(p.getAllowance()));
                row.createCell(c++).setCellValue(d(p.getGrossPay()));
                row.createCell(c++).setCellValue(d(p.getTotalBonus()));
                row.createCell(c++).setCellValue(d(p.getTotalDeduction()));
                row.createCell(c).setCellValue(d(p.getNetPay()));
            }

            Row total = sheet.createRow(r + 1);
            Cell tl = total.createCell(0);
            tl.setCellValue("TỔNG CỘNG (" + slips.size() + " nhân viên)");
            tl.setCellStyle(bold);
            Cell tn = total.createCell(14);
            tn.setCellValue(slips.stream().map(Payslip::getNetPay)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
            tn.setCellStyle(bold);

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi xuất Excel bảng lương: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    //  PDF — PHIẾU LƯƠNG CÁ NHÂN
    // =====================================================================
    public byte[] exportPayslipPdf(Long payslipId) {
        Payslip p = payslipRepository.findById(payslipId)
                .orElseThrow(() -> NotFoundException.of("phiếu lương", payslipId));
        StoreContext.assertSameStore(p.getPeriod().getStore().getId());   // chống IDOR đa chi nhánh
        // STAFF chỉ in được phiếu ĐÃ CHỐT của CHÍNH mình (ADMIN/MANAGER in mọi phiếu trong phạm vi).
        CustomUserDetails me = SecurityUtils.currentUser();
        boolean isManager = "ADMIN".equals(me.getRole()) || "MANAGER".equals(me.getRole());
        PayrollStatus st = p.getPeriod().getStatus();
        boolean finalized = st == PayrollStatus.APPROVED || st == PayrollStatus.PAID;
        if (!isManager && (!p.getUser().getId().equals(me.getId()) || !finalized)) {
            throw new BadRequestException("Bạn chỉ xem được phiếu lương đã chốt của chính mình");
        }
        List<PayslipAdjustment> adjustments = adjustmentRepository.findByPayslipIdOrderByCreatedAt(payslipId);
        StoreConfig cfg = storeConfigRepository.findById(p.getPeriod().getStore().getId()).orElse(null);

        Document doc = new Document(PageSize.A5, 28, 28, 24, 24);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            BaseFont bf = loadUnicodeFont();
            Font fTitle = new Font(bf, 14, Font.BOLD);
            Font fSub = new Font(bf, 9, Font.NORMAL);
            Font fNormal = new Font(bf, 10, Font.NORMAL);
            Font fBold = new Font(bf, 10, Font.BOLD);
            Font fBig = new Font(bf, 12, Font.BOLD);

            String storeName = cfg != null && cfg.getName() != null ? cfg.getName() : p.getPeriod().getStore().getName();
            addCenter(doc, storeName.toUpperCase(), fBold);
            if (cfg != null && cfg.getAddress() != null) addCenter(doc, cfg.getAddress(), fSub);
            addCenter(doc, " ", fSub);
            addCenter(doc, "PHIẾU LƯƠNG THÁNG " + p.getPeriod().getPeriodMonth(), fTitle);
            addCenter(doc, " ", fSub);

            Paragraph emp = new Paragraph("Nhân viên: " + p.getUser().getFullName(), fNormal);
            doc.add(emp);
            doc.add(new Paragraph("Loại lương: "
                    + (p.getPayType() == PayType.HOURLY ? "Theo giờ" : "Theo tháng")
                    + " · Đơn giá: " + MONEY.format(p.getBaseRate())
                    + (p.getPayType() == PayType.HOURLY ? "đ/giờ" : "đ/tháng"), fSub));
            doc.add(new Paragraph(" ", fSub));

            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(100);
            try { t.setWidths(new float[]{3f, 2f}); } catch (DocumentException ignored) {}
            kv(t, "Số ca làm", p.getShiftCount() + " ca", fNormal, fNormal);
            kv(t, "Giờ công", num(p.getWorkedHours()) + " h", fNormal, fNormal);
            kv(t, "Giờ thường", num(p.getRegularHours()) + " h", fNormal, fNormal);
            kv(t, "Giờ tăng ca", num(p.getOtHours()) + " h", fNormal, fNormal);
            kv(t, "Lương gốc", MONEY.format(p.getRegularPay()) + " đ", fNormal, fNormal);
            kv(t, "Tiền tăng ca", MONEY.format(p.getOtPay()) + " đ", fNormal, fNormal);
            kv(t, "Phụ cấp", MONEY.format(p.getAllowance()) + " đ", fNormal, fNormal);
            kv(t, "Lương gộp", MONEY.format(p.getGrossPay()) + " đ", fBold, fBold);
            for (PayslipAdjustment a : adjustments) {
                boolean bonus = a.getType() == PayslipAdjustmentType.BONUS;
                kv(t, (bonus ? "(+) " : "(−) ") + a.getReason(),
                        (bonus ? "+" : "−") + MONEY.format(a.getAmount()) + " đ", fSub, fSub);
            }
            doc.add(t);
            doc.add(new Paragraph(" ", fSub));

            PdfPTable net = new PdfPTable(2);
            net.setWidthPercentage(100);
            try { net.setWidths(new float[]{3f, 2f}); } catch (DocumentException ignored) {}
            PdfPCell k = new PdfPCell(new Phrase("THỰC LĨNH", fBig));
            k.setBorder(Rectangle.TOP);
            k.setPadding(6);
            PdfPCell v = new PdfPCell(new Phrase(MONEY.format(p.getNetPay()) + " đ", fBig));
            v.setBorder(Rectangle.TOP);
            v.setHorizontalAlignment(Element.ALIGN_RIGHT);
            v.setPadding(6);
            net.addCell(k);
            net.addCell(v);
            doc.add(net);

            doc.add(new Paragraph(" ", fSub));
            Paragraph note = new Paragraph(
                    "Giờ công tổng hợp tự động từ ca làm việc và chấm công trong tháng. "
                    + "Phiếu phát hành điện tử từ hệ thống POS.", fSub);
            note.setAlignment(Element.ALIGN_CENTER);
            doc.add(note);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi xuất PDF phiếu lương: " + e.getMessage(), e);
        }
    }

    // --- hỗ trợ ---
    private static double d(BigDecimal v) {
        return v != null ? v.doubleValue() : 0d;
    }

    private static String num(BigDecimal v) {
        return v != null ? v.stripTrailingZeros().toPlainString() : "0";
    }

    private void kv(PdfPTable t, String key, String value, Font kf, Font vf) {
        PdfPCell k = new PdfPCell(new Phrase(key, kf));
        k.setBorder(Rectangle.NO_BORDER);
        k.setPadding(3);
        PdfPCell v = new PdfPCell(new Phrase(value, vf));
        v.setBorder(Rectangle.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(3);
        t.addCell(k);
        t.addCell(v);
    }

    private void addCenter(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    /** Font Unicode để render tiếng Việt (giống InvoicePdfService). */
    private BaseFont loadUnicodeFont() {
        String[] candidates = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/segoeui.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        };
        try {
            for (String path : candidates) {
                if (new File(path).exists()) {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            }
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("Không nạp được font PDF: " + e.getMessage(), e);
        }
    }
}
