package com.pos.service;

import com.pos.dto.user.CreateUserRequest;
import com.pos.dto.user.ResetPasswordRequest;
import com.pos.dto.user.UpdateUserRequest;
import com.pos.dto.user.UserResponse;
import com.pos.entity.Store;
import com.pos.entity.User;
import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
import com.pos.repository.StoreRepository;
import com.pos.repository.UserRepository;
import com.pos.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý tài khoản nhân viên & phân quyền (FR1.3 - UC02, chỉ Admin). */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository repository, StoreRepository storeRepository,
                       PasswordEncoder passwordEncoder, AuditService auditService) {
        this.repository = repository;
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * Chốt cửa hàng cho tài khoản theo vai trò (đa cửa hàng). Chỉ ADMIN toàn chuỗi mới quản lý tài khoản
     * (xem @PreAuthorize ở UserController), nên người tạo luôn là ADMIN:
     * <ul>
     *   <li>{@code ADMIN} → KHÔNG gắn cửa hàng (quản trị toàn chuỗi).</li>
     *   <li>{@code MANAGER}/{@code STAFF} → BẮT BUỘC chọn một cửa hàng.</li>
     * </ul>
     */
    private Store resolveStore(Role role, Long requestedStoreId) {
        if (role == Role.ADMIN) return null;   // quản trị toàn chuỗi — không thuộc cửa hàng nào
        if (requestedStoreId == null) {
            throw new BadRequestException("Vai trò " + role + " phải gắn một cửa hàng");
        }
        return storeRepository.findById(requestedStoreId)
                .orElseThrow(() -> NotFoundException.of("cửa hàng", requestedStoreId));
    }

    /**
     * Người gọi có phải MANAGER (bị giới hạn phạm vi) không. Controller chỉ cho ADMIN/MANAGER vào,
     * nên không-phải-ADMIN nghĩa là MANAGER. MANAGER chỉ được quản lý STAFF trong cửa hàng của mình.
     */
    private boolean isManagerScope() {
        return !SecurityUtils.isAdmin();
    }

    /** Cửa hàng của manager đang đăng nhập (luôn != null vì MANAGER bắt buộc gắn cửa hàng). */
    private Long managerStoreId() {
        return SecurityUtils.currentUser().getStoreId();
    }

    /**
     * Chốt phạm vi MANAGER: chỉ được thao tác lên STAFF thuộc CHÍNH cửa hàng mình.
     * ADMIN bỏ qua kiểm tra (toàn quyền). Vi phạm → từ chối.
     */
    private void assertManagedStaff(User target) {
        if (!isManagerScope()) return;
        Long myStore = managerStoreId();
        boolean sameStore = target.getStore() != null && target.getStore().getId().equals(myStore);
        if (target.getRole() != Role.STAFF || !sameStore) {
            throw new BadRequestException("Không có quyền với tài khoản này");
        }
    }

    public List<UserResponse> findAll() {
        List<User> users = isManagerScope()
                // manager: CHỈ thấy STAFF của cửa hàng mình (khớp với phạm vi được phép thao tác,
                // không lộ tài khoản manager/admin khác trong cùng cửa hàng).
                ? repository.findByStore_IdAndRoleOrderByIdAsc(managerStoreId(), Role.STAFF)
                : repository.findAll();                                     // admin: toàn chuỗi
        return users.stream().map(UserResponse::from).toList();
    }

    /** Chặn thao tác làm MẤT quản trị viên cuối cùng (hạ quyền hoặc khóa admin duy nhất còn hoạt động). */
    private void assertNotLastActiveAdmin(User target) {
        if (target.getRole() == Role.ADMIN
                && target.getStatus() == UserStatus.ACTIVE
                && repository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new BadRequestException(
                    "Không thể hạ quyền hoặc khóa quản trị viên cuối cùng — hãy tạo/ cấp quyền một quản trị viên khác trước");
        }
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (repository.existsByUsername(req.username())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại: " + req.username());
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setFullName(req.fullName());
        if (isManagerScope()) {
            // MANAGER chỉ tạo được STAFF cho CHÍNH cửa hàng mình — bỏ qua role/storeId client gửi.
            u.setRole(Role.STAFF);
            u.setStore(resolveStore(Role.STAFF, managerStoreId()));
        } else {
            u.setRole(req.role());
            u.setStore(resolveStore(req.role(), req.storeId()));
        }
        u.setStatus(UserStatus.ACTIVE);
        User saved = repository.save(u);
        auditService.log("CREATE_USER", "USER", saved.getId(),
                "Tạo tài khoản " + saved.getUsername() + " (vai trò " + saved.getRole()
                        + (saved.getStore() != null ? ", chi nhánh " + saved.getStore().getName() : "") + ")");
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        User u = getOrThrow(id);
        assertManagedStaff(u);   // MANAGER: chỉ sửa được STAFF trong cửa hàng mình
        // Không cho tự khóa chính mình để tránh mất quyền truy cập
        if (u.getId().equals(SecurityUtils.currentUserId()) && req.status() == UserStatus.LOCKED) {
            throw new BadRequestException("Không thể tự khóa tài khoản của chính mình");
        }
        // Không để mất quản trị viên cuối cùng (hạ quyền khỏi ADMIN hoặc khóa).
        if (!isManagerScope() && (req.role() != Role.ADMIN || req.status() == UserStatus.LOCKED)) {
            assertNotLastActiveAdmin(u);
        }
        u.setFullName(req.fullName());
        if (!isManagerScope()) {
            // Chỉ ADMIN mới đổi được vai trò/cửa hàng; MANAGER giữ nguyên STAFF + cửa hàng cũ.
            u.setRole(req.role());
            u.setStore(resolveStore(req.role(), req.storeId()));
        }
        u.setStatus(req.status());
        User saved = repository.save(u);
        auditService.log("UPDATE_USER", "USER", saved.getId(),
                "Cập nhật tài khoản " + saved.getUsername() + " → vai trò " + saved.getRole()
                        + ", trạng thái " + saved.getStatus());
        return UserResponse.from(saved);
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req) {
        User u = getOrThrow(id);
        assertManagedStaff(u);   // MANAGER: chỉ đặt lại mật khẩu STAFF trong cửa hàng mình
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        repository.save(u);
        auditService.log("RESET_PASSWORD", "USER", id, "Đặt lại mật khẩu tài khoản " + u.getUsername());
    }

    /** Khóa tài khoản (xóa mềm) — không xóa cứng vì còn tham chiếu phiếu nhập/ca. */
    @Transactional
    public void lock(Long id) {
        User u = getOrThrow(id);
        assertManagedStaff(u);   // MANAGER: chỉ khóa STAFF trong cửa hàng mình
        if (u.getId().equals(SecurityUtils.currentUserId())) {
            throw new BadRequestException("Không thể tự khóa tài khoản của chính mình");
        }
        assertNotLastActiveAdmin(u);   // không khóa quản trị viên cuối cùng
        u.setStatus(UserStatus.LOCKED);
        repository.save(u);
        auditService.log("LOCK_USER", "USER", id, "Khóa tài khoản " + u.getUsername());
    }

    private User getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("tài khoản", id));
    }
}
