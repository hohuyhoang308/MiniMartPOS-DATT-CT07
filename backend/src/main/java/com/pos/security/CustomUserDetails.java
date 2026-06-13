package com.pos.security;

import com.pos.entity.User;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Bọc {@link User} cho Spring Security. Quyền = "ROLE_" + role để dùng hasRole(...). */
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public String getRole() {
        return user.getRole().name();
    }

    /** Chi nhánh người dùng trực thuộc; null = quản trị toàn chuỗi (CHAIN_ADMIN). */
    public Long getStoreId() {
        return user.getStore() != null ? user.getStore().getId() : null;
    }

    /** Tên chi nhánh trực thuộc (null nếu là CHAIN_ADMIN). */
    public String getStoreName() {
        return user.getStore() != null ? user.getStore().getName() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /** Cửa hàng còn hoạt động? ADMIN không gắn cửa hàng → luôn true. */
    private boolean storeActive() {
        return user.getStore() == null || user.getStore().getStatus() == CommonStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == UserStatus.ACTIVE && storeActive();
    }

    /** Tài khoản đăng nhập/hoạt động được khi: tài khoản ACTIVE **và** cửa hàng trực thuộc còn hoạt động.
     *  → Cửa hàng đóng (INACTIVE) thì MANAGER/STAFF của cửa hàng đó KHÔNG đăng nhập được (và token cũ bị vô hiệu). */
    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE && storeActive();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
