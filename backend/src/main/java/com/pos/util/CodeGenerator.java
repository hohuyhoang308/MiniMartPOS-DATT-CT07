package com.pos.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Sinh mã chứng từ dạng &lt;PREFIX&gt;&lt;yyyyMMdd&gt;-&lt;seq&gt; (vd HD20260606-0001). */
public final class CodeGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CodeGenerator() {}

    /** Tiền tố + ngày hôm nay, vd "HD20260606-" — dùng để đếm số chứng từ trong ngày. */
    public static String datePrefix(String prefix) {
        return prefix + LocalDate.now().format(DATE_FMT) + "-";
    }

    /** Ghép mã hoàn chỉnh với số thứ tự đã đệm 0. */
    public static String build(String prefix, long sequence, int padding) {
        return datePrefix(prefix) + String.format("%0" + padding + "d", sequence);
    }
}
