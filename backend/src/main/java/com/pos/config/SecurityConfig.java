package com.pos.config;

import com.pos.security.JwtAuthEntryPoint;
import com.pos.security.JwtAuthenticationFilter;
import com.pos.security.StoreContextFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình bảo mật (NFR4): stateless JWT, BCrypt, phân quyền theo vai trò (@PreAuthorize),
 * CORS cho origin của frontend React.
 */
@Configuration
@EnableMethodSecurity   // bật @PreAuthorize ở tầng Controller/Service
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final StoreContextFilter storeContextFilter;
    private final JwtAuthEntryPoint authEntryPoint;
    private final String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          StoreContextFilter storeContextFilter,
                          JwtAuthEntryPoint authEntryPoint,
                          @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.storeContextFilter = storeContextFilter;
        this.authEntryPoint = authEntryPoint;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()  // health check cho Docker/K8s
                .anyRequest().authenticated())
            .exceptionHandling(eh -> eh.authenticationEntryPoint(authEntryPoint))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // Sau khi đã xác thực: nạp cửa hàng đang chọn (X-Store-Id) cho ADMIN toàn chuỗi.
            .addFilterAfter(storeContextFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Dùng allowedOriginPatterns (thay cho allowedOrigins) để khớp được cả
        // localhost / 127.0.0.1 / IP LAN khi bật allowCredentials(true).
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Phân cấp vai trò TỪ CAO XUỐNG THẤP: ADMIN ⊃ MANAGER ⊃ STAFF.
     * Nhờ vậy quyền cao tự động bao hàm quyền thấp ở @PreAuthorize (ADMIN làm được mọi việc của
     * MANAGER; MANAGER làm được việc của STAFF).
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_MANAGER
                ROLE_MANAGER > ROLE_STAFF
                """);
    }

    /** Áp phân cấp vai trò vào @PreAuthorize (method security). */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
