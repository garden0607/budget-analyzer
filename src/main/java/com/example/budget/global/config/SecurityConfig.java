package com.example.budget.global.config;   // global/config 패키지

import com.example.budget.global.exception.ErrorCode;                    // 오류 목록표 (401 문구 가져오기)
import com.example.budget.global.security.JwtAuthenticationFilter;       // 우리가 만든 토큰 검사 필터
import com.example.budget.global.security.JwtProvider;                   // 토큰 검증 도구 (필터에 넘겨줌)
import lombok.RequiredArgsConstructor;                                   // final 필드 생성자 자동 생성
import org.springframework.context.annotation.Bean;                      // 스프링에 객체 등록
import org.springframework.context.annotation.Configuration;             // 설정 클래스 표시
import org.springframework.security.config.annotation.web.builders.HttpSecurity;              // 보안 규칙을 만드는 도구
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;   // Spring Security 사용 표시
import org.springframework.security.config.http.SessionCreationPolicy;   // 세션 사용 방식
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCrypt 암호화 구현체
import org.springframework.security.crypto.password.PasswordEncoder;     // 비밀번호 암호화 도구 타입
import org.springframework.security.web.SecurityFilterChain;             // 보안 검사 규칙 목록
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // 스프링 기본 로그인 필터

// 보안 규칙 전체를 정하는 설정 클래스
@Configuration           // 스프링이 읽는 설정 파일로 등록
@EnableWebSecurity       // Spring Security 기능 켜기
@RequiredArgsConstructor // jwtProvider를 받는 생성자 자동 생성
public class SecurityConfig {

    private final JwtProvider jwtProvider;   // 필터를 만들 때 넘겨줄 토큰 도구

    @Bean   // 아래에서 만든 보안 규칙을 스프링에 등록
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. JWT 방식이라 필요 없는 기본 기능 끄기
                .csrf(csrf -> csrf.disable())              // CSRF 방어 끔 (쿠키·세션 대상 공격이라 헤더 토큰 방식엔 해당 적음)
                .formLogin(form -> form.disable())         // 스프링 기본 로그인 화면 끔
                .httpBasic(basic -> basic.disable())       // 브라우저 팝업 로그인 끔
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 안 만듦 (서버가 로그인 상태 저장 X)

                // 2. 주소별 접근 규칙 (위에서부터 순서대로 검사)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger: 누구나
                        .requestMatchers("/api/auth/**").permitAll()                       // 회원가입·로그인: 누구나 (아직 토큰이 없으니까)
                        .requestMatchers("/api/**").authenticated()                        // 나머지 API: 로그인 필요
                        .anyRequest().permitAll())                                         // 화면(HTML): 누구나 (데이터는 API가 지킴)

                // 3. 로그인 안 한 사람이 API 요청 시 → 401 JSON 응답
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {   // 인증 실패 시 실행
                    ErrorCode code = ErrorCode.UNAUTHORIZED;                                     // 401 "로그인이 필요합니다."
                    response.setStatus(code.getStatus().value());                                // 상태 코드 401 설정
                    response.setContentType("application/json;charset=UTF-8");                   // JSON + 한글 깨짐 방지
                    response.getWriter().write(                                                  // { code, message } 직접 작성
                            "{\"code\":\"" + code.name() + "\",\"message\":\"" + code.getMessage() + "\"}");
                }))   // 필터 단계 오류라 GlobalExceptionHandler가 못 잡음 → 여기서 같은 형식으로 응답

                // 4. 우리 JWT 필터를 스프링 기본 로그인 필터 앞에 끼우기
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),     // 필터를 여기서 직접 한 번만 등록
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();   // 위 규칙들로 보안 설정 완성
    }

    @Bean   // 비밀번호 암호화 도구 등록 → AuthService에서 주입받아 encode(), matches() 사용
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();   // BCrypt 방식 (단방향, salt 포함, 결과 60글자)
    }
}