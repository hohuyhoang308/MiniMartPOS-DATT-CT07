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

    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(UserResponse::from).toList();
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
        u.setRole(req.role());
        u.setStore(resolveStore(req.role(), req.storeId()));
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
        // Không cho tự khóa chính mình để tránh mất quyền truy cập
        if (u.getId().equals(SecurityUtils.currentUserId()) && req.status() == UserStatus.LOCKED) {
            throw new BadRequestException("Không thể tự khóa tài khoản của chính mình");
        }
        u.setFullName(req.fullName());
        u.setRole(req.role());
        u.setStore(resolveStore(req.role(), req.storeId()));
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
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        repository.save(u);
        auditService.log("RESET_PASSWORD", "USER", id, "Đặt lại mật khẩu tài khoản " + u.getUsername());
    }

    /** Khóa tài khoản (xóa mềm) — không xóa cứng vì còn tham chiếu phiếu nhập/ca. */
    @Transactional
    public void lock(Long id) {
        User u = getOrThrow(id);
        if (u.getId().equals(SecurityUtils.currentUserId())) {
            throw new BadRequestException("Không thể tự khóa tài khoản của chính mình");
        }
        u.setStatus(UserStatus.LOCKED);
        repository.save(u);
        auditService.log("LOCK_USER", "USER", id, "Khóa tài khoản " + u.getUsername());
    }

    private User getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("tài khoản", id));
    }
}
