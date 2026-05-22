package com.pos.service;

import com.pos.dto.user.CreateUserRequest;
import com.pos.dto.user.ResetPasswordRequest;
import com.pos.dto.user.UpdateUserRequest;
import com.pos.dto.user.UserResponse;
import com.pos.entity.User;
import com.pos.entity.enums.UserStatus;
import com.pos.exception.BadRequestException;
import com.pos.exception.NotFoundException;
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
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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
        u.setStatus(UserStatus.ACTIVE);
        return UserResponse.from(repository.save(u));
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
        u.setStatus(req.status());
        return UserResponse.from(repository.save(u));
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req) {
        User u = getOrThrow(id);
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        repository.save(u);
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
    }

    private User getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("tài khoản", id));
    }
}
