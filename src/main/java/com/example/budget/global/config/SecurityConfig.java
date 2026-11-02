package com.example.budget.global.config;

import com.example.budget.global.exception.ErrorCode;
import com.example.budget.global.security.JwtAuthenticationFilter;
import com.example.budget.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. JWT 방식이라 필요 없는 기본 기능 끄기
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. 주소별 접근 규칙
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger
                        .requestMatchers("/api/auth/**").permitAll()                       // 회원가입·로그인
                        .requestMatchers("/api/**").authenticated()                        // 나머지 API는 로그인 필요
                        .anyRequest().permitAll())                                         // 화면(HTML)은 허용

                // 3. 로그인 안 한 사람이 API 요청 시 → 401 JSON 응답
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
                    ErrorCode code = ErrorCode.UNAUTHORIZED;
                    response.setStatus(code.getStatus().value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"code\":\"" + code.name() + "\",\"message\":\"" + code.getMessage() + "\"}");
                }))

                // 4. 우리가 만든 JWT 필터를 기본 로그인 필터 앞에 끼우기
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}