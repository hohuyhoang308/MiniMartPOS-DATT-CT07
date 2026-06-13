package com.pos.repository;

import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    /** Ca đang mở của một thu ngân (mỗi thu ngân chỉ nên có 1 ca OPEN). */
    Optional<WorkShift> findFirstByUserIdAndStatusOrderByOpenedAtDesc(Long userId, ShiftStatus status);

    boolean existsByUserIdAndStatus(Long userId, ShiftStatus status);

    /** 200 ca gần nhất (mới nhất trước) — màn Quản lý ca; giới hạn ở DB để không tải vô hạn. */
    List<WorkShift> findTop200ByOrderByOpenedAtDesc();

    /** 200 ca gần nhất của MỘT chi nhánh (đa chuỗi) — quản lý chỉ thấy ca chi nhánh mình. */
    List<WorkShift> findTop200ByStoreIdOrderByOpenedAtDesc(Long storeId);

    /** Ca đã đóng gần nhất — để gợi ý tiền đầu ca (= tiền cuối ca trước, két chuyển tiếp). */
    Optional<WorkShift> findFirstByStatusOrderByClosedAtDesc(ShiftStatus status);

    /** Ca đã đóng gần nhất CỦA MỘT CỬA HÀNG — gợi ý tiền đầu ca đúng két của cửa hàng (đa cửa hàng). */
    Optional<WorkShift> findFirstByStoreIdAndStatusOrderByClosedAtDesc(Long storeId, ShiftStatus status);
}
