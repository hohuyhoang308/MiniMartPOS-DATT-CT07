package com.pos.dto.report;

/** Kỳ gộp báo cáo doanh thu/lợi nhuận, kèm chuỗi định dạng MySQL DATE_FORMAT tương ứng. */
public enum ReportPeriod {
    DAY("%Y-%m-%d"),
    WEEK("%x-W%v"),   // tuần ISO: %x = năm ISO, %v = tuần 01-53
    MONTH("%Y-%m"),
    YEAR("%Y");

    private final String sqlFormat;

    ReportPeriod(String sqlFormat) {
        this.sqlFormat = sqlFormat;
    }

    public String sqlFormat() {
        return sqlFormat;
    }

    /** Parse an toàn từ query param (mặc định DAY nếu null/không hợp lệ). */
    public static ReportPeriod parse(String raw) {
        if (raw == null || raw.isBlank()) return DAY;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DAY;
        }
    }
}
