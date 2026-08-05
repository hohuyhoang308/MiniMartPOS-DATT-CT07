package com.pos.service;

import com.pos.dto.auth.LoginRequest;
import com.pos.dto.auth.LoginResponse;
import com.pos.entity.Store;
import com.pos.entity.User;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;
import com.pos.exception.TooManyRequestsException;
import com.pos.security.CustomUserDetails;
import com.pos.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử XÁC THỰC (FR1, UC01): chống dò mật khẩu theo username (khóa 60 giây sau 5 lần sai),
 * chống rải mật khẩu theo IP (khóa 5 phút sau 20 lần sai), đăng nhập thành công trả JWT
 * và xóa bộ đếm sai. Không cần CSDL — AuthenticationManager được mock.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;

    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(authenticationManager, jwtService);
    }

    private LoginRequest req(String username) {
        return new LoginRequest(username, "sai-mat-khau");
    }

    private Authentication successFor(String username) {
        Store store = new Store();
        store.setId(1L);
        store.setName("CH01");
        store.setStatus(CommonStatus.ACTIVE);
        User u = new User();
        u.setId(10L);
        u.setUsername(username);
        u.setFullName("Nhân viên A");
        u.setRole(Role.STAFF);
        u.setStatus(UserStatus.ACTIVE);
        u.setStore(store);
        return new UsernamePasswordAuthenticationToken(new CustomUserDetails(u), null, List.of());
    }

    @Test
    void locks_username_for_60s_after_five_consecutive_failures() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        for (int i = 0; i < 5; i++) {
            Throwable t = catchThrowable(() -> service.login(req("staff"), "1.2.3.4"));
            assertThat(t).isInstanceOf(BadCredentialsException.class); // 5 lần đầu: sai mật khẩu bình thường
        }
        // Lần thứ 6: bị khóa tạm — trả 429 kèm số giây chờ, KHÔNG chạm vào so khớp mật khẩu nữa
        assertThatThrownBy(() -> service.login(req("staff"), "1.2.3.4"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("giây");
    }

    @Test
    void locks_ip_after_twenty_failures_spread_across_usernames() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        // Rải 1 mật khẩu qua 20 username khác nhau từ CÙNG một IP (spray) — mỗi username mới chưa bị khóa riêng
        for (int i = 0; i < 20; i++) {
            String username = "user" + i;
            catchThrowable(() -> service.login(req(username), "9.9.9.9"));
        }
        assertThatThrownBy(() -> service.login(req("user-moi"), "9.9.9.9"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("thiết bị");
    }

    @Test
    void successful_login_returns_jwt_with_role_and_store() {
        when(authenticationManager.authenticate(any())).thenReturn(successFor("staff"));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        LoginResponse resp = service.login(new LoginRequest("staff", "123456"), "1.2.3.4");

        assertThat(resp.token()).isEqualTo("jwt-token");
        assertThat(resp.role()).isEqualTo("STAFF");
        assertThat(resp.storeId()).isEqualTo(1L); // JWT gắn cửa hàng → nền tảng cô lập dữ liệu
    }

    @Test
    void successful_login_resets_failure_counter() {
        // 4 lần sai → chưa khóa; đăng nhập đúng → xóa bộ đếm; sai tiếp 1 lần vẫn là BadCredentials (không phải 429)
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"))
                .thenThrow(new BadCredentialsException("bad"))
                .thenThrow(new BadCredentialsException("bad"))
                .thenThrow(new BadCredentialsException("bad"))
                .thenReturn(successFor("staff"))
                .thenThrow(new BadCredentialsException("bad"));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        for (int i = 0; i < 4; i++) {
            catchThrowable(() -> service.login(req("staff"), "1.2.3.4"));
        }
        assertThat(service.login(new LoginRequest("staff", "dung"), "1.2.3.4").token()).isEqualTo("jwt-token");
        assertThatThrownBy(() -> service.login(req("staff"), "1.2.3.4"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
