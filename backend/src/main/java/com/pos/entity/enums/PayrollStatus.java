package com.pos.entity.enums;

/** Vòng đời một kỳ lương (chi nhánh × tháng) — quy trình duyệt 2 bước. */
public enum PayrollStatus {
    /** Đang soạn: được tính lại từ ca/chấm công + thêm/xóa thưởng-phạt. */
    DRAFT,
    /** Đã TRÌNH duyệt: người lập gửi duyệt; số liệu đóng băng, chờ cấp trên duyệt. */
    PENDING_APPROVAL,
    /** Đã DUYỆT: cấp có thẩm quyền (ADMIN) duyệt — số liệu chốt cuối, sẵn sàng chi. */
    APPROVED,
    /** Đã CHI lương — trạng thái cuối, chỉ lưu vết. */
    PAID
}
