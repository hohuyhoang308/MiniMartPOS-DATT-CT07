package com.pos.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử PHÂN QUYỀN PHÂN TẦNG (FR1.2): cấu hình Role Hierarchy phải cho
 * ADMIN ⊃ MANAGER ⊃ STAFF (vai trò cao có trọn quyền vai trò thấp) và
 * chiều ngược lại KHÔNG được leo thang (STAFF không với tới quyền MANAGER).
 */
class RoleHierarchyTest {

    private final RoleHierarchy hierarchy = SecurityConfig.roleHierarchy();

    private List<String> reachableFrom(String role) {
        Collection<? extends GrantedAuthority> reached =
                hierarchy.getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority(role)));
        return reached.stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void admin_inherits_manager_and_staff_permissions() {
        assertThat(reachableFrom("ROLE_ADMIN"))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF");
    }

    @Test
    void manager_inherits_staff_but_not_admin() {
        List<String> reached = reachableFrom("ROLE_MANAGER");

        assertThat(reached).containsExactlyInAnyOrder("ROLE_MANAGER", "ROLE_STAFF");
        assertThat(reached).doesNotContain("ROLE_ADMIN"); // không leo thang lên trên
    }

    @Test
    void staff_cannot_escalate_to_any_higher_role() {
        assertThat(reachableFrom("ROLE_STAFF")).containsExactly("ROLE_STAFF");
    }
}
