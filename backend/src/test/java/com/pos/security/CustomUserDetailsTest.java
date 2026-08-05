package com.pos.security;

import com.pos.entity.Store;
import com.pos.entity.User;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử PHÂN QUYỀN ở lớp tài khoản (FR1, BR-16): quyền ROLE_* cấp đúng theo vai trò;
 * tài khoản LOCKED hoặc thuộc cửa hàng ĐÃ ĐÓNG (INACTIVE) không đăng nhập được;
 * ADMIN toàn chuỗi (không gắn cửa hàng) không bị ràng buộc trạng thái cửa hàng.
 */
class CustomUserDetailsTest {

    private User user(Role role, UserStatus status, CommonStatus storeStatus) {
        User u = new User();
        u.setUsername("u");
        u.setRole(role);
        u.setStatus(status);
        if (storeStatus != null) {
            Store s = new Store();
            s.setId(1L);
            s.setStatus(storeStatus);
            u.setStore(s);
        }
        return u;
    }

    @Test
    void grants_single_role_authority_with_prefix() {
        CustomUserDetails d = new CustomUserDetails(user(Role.STAFF, UserStatus.ACTIVE, CommonStatus.ACTIVE));

        assertThat(d.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_STAFF");
    }

    @Test
    void staff_of_inactive_store_cannot_login() {
        // BR-16: đóng cửa hàng ⇒ nhân viên cửa hàng đó bị chặn đăng nhập (token cũ cũng vô hiệu)
        CustomUserDetails d = new CustomUserDetails(user(Role.STAFF, UserStatus.ACTIVE, CommonStatus.INACTIVE));

        assertThat(d.isEnabled()).isFalse();
        assertThat(d.isAccountNonLocked()).isFalse();
    }

    @Test
    void locked_account_is_disabled_even_when_store_active() {
        CustomUserDetails d = new CustomUserDetails(user(Role.STAFF, UserStatus.LOCKED, CommonStatus.ACTIVE));

        assertThat(d.isEnabled()).isFalse();
    }

    @Test
    void chain_admin_without_store_is_not_bound_by_store_status() {
        CustomUserDetails d = new CustomUserDetails(user(Role.ADMIN, UserStatus.ACTIVE, null));

        assertThat(d.isEnabled()).isTrue();
        assertThat(d.getStoreId()).isNull(); // phạm vi toàn chuỗi
    }
}
