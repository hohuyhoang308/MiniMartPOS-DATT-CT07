package com.pos.util;

import com.lowagie.text.pdf.BaseFont;

import java.io.File;

/** Nạp font Unicode cho PDF (tiếng Việt). Dùng chung cho mọi service xuất PDF. */
public final class PdfFonts {

    // Ứng viên theo thứ tự ưu tiên: Arial/Segoe (Windows) rồi DejaVu (Linux/Docker).
    private static final String[] CANDIDATES = {
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/segoeui.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    };

    private PdfFonts() {
    }

    /** Font Unicode để render tiếng Việt; không có font hệ thống thì fallback Helvetica. */
    public static BaseFont unicode() {
        for (String path : CANDIDATES) {
            try {
                if (new File(path).exists()) {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {
                // font hỏng/không đọc được → thử ứng viên tiếp theo
            }
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("Không nạp được font cho PDF", e);
        }
    }
}
