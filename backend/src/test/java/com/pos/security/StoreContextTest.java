package com.pos.security;

import com.pos.entity.Store;
import com.pos.entity.User;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;
import com.pos.exception.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử CÔ LẬP DỮ LIỆU ĐA CỬA HÀNG (BR-09): người dùng gắn cửa hàng luôn bị chốt theo
 * cửa hàng của mình (header X-Store-Id bị bỏ qua — không thể tự vượt rào); ADMIN toàn chuỗi
 * chọn cửa hàng qua header; assertSameStore chặn truy cập bản ghi chéo cửa hàng (IDOR).
 */
class StoreContextTest {

    @AfterEach
    void tearDown() {
        StoreContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Role role, Long storeId) {
        User u = new User();
        u.setId(10L);
        u.setUsername("u");
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        if (storeId != null) {
            Store s = new Store();
            s.setId(storeId);
            s.setStatus(CommonStatus.ACTIVE);
            u.setStore(s);
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(u), null, List.of()));
    }

    @Test
    void bound_user_ignores_header_and_stays_in_own_store() {
        loginAs(Role.STAFF, 1L);
        StoreContext.setHeaderStoreId(2L); // cố tình gửi header trỏ sang cửa hàng khác

        assertThat(StoreContext.currentStoreId()).isEqualTo(1L); // vẫn bị chốt theo cửa hàng của mình
    }

    @Test
    void chain_admin_switches_store_via_header() {
        loginAs(Role.ADMIN, null);
        StoreContext.setHeaderStoreId(2L);

        assertThat(StoreContext.currentStoreId()).isEqualTo(2L);
    }

    @Test
    void write_operations_require_a_selected_store() {
        loginAs(Role.ADMIN, null); // ADMIN chưa chọn cửa hàng → được ĐỌC toàn chuỗi nhưng cấm GHI

        assertThatThrownBy(StoreContext::requireStoreId)
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("chi nhánh");
    }

    @Test
    void assert_same_store_blocks_cross_store_record_access() {
        loginAs(Role.MANAGER, 1L);

        assertThatCode(() -> StoreContext.assertSameStore(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> StoreContext.assertSameStore(2L)) // bản ghi của cửa hàng khác (IDOR)
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void chain_admin_without_selection_reads_all_stores() {
        loginAs(Role.ADMIN, null); // currentStoreId = null = phạm vi toàn chuỗi

        assertThatCode(() -> StoreContext.assertSameStore(2L)).doesNotThrowAnyException();
    }
}
