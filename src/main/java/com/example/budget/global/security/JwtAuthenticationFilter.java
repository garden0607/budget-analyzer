package com.example.budget.global.security;   // global/security 패키지

import jakarta.servlet.FilterChain;                   // 다음 필터(또는 Controller)로 넘기는 통로
import jakarta.servlet.ServletException;              // 필터 처리 중 오류
import jakarta.servlet.http.HttpServletRequest;       // 들어온 요청 (헤더, 주소 등)
import jakarta.servlet.http.HttpServletResponse;      // 나갈 응답
import lombok.RequiredArgsConstructor;                // final 필드 생성자 자동 생성
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // "인증된 사용자" 정보 객체
import org.springframework.security.core.context.SecurityContextHolder;                 // 현재 요청의 로그인 정보 메모장
import org.springframework.web.filter.OncePerRequestFilter;                              // 요청당 한 번만 실행되는 필터

import java.io.IOException;   // 입출력 오류
import java.util.List;        // 권한 목록 (지금은 빈 목록)

// 요청마다 토큰을 검사해서, 유효하면 "n번 회원의 요청"이라고 기록하는 필터
@RequiredArgsConstructor                                       // jwtProvider를 받는 생성자 자동 생성
public class JwtAuthenticationFilter extends OncePerRequestFilter {   // @Component 없음 → SecurityConfig에서 직접 등록

    private final JwtProvider jwtProvider;   // 토큰 검증·회원 id 꺼내기 담당

    @Override
    protected void doFilterInternal(HttpServletRequest request,     // 요청이 올 때마다 실행되는 메서드
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);   // 1. 헤더에서 토큰 꺼내기 (없으면 null)

        if (token != null && jwtProvider.validateToken(token)) {   // 2. 토큰이 있고, 서명·만료 검사 통과하면
            Long userId = jwtProvider.getUserId(token);            //    토큰에서 회원 id 꺼내기
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of()); // (누구=회원 id, 비밀번호=없음, 권한=없음)
            SecurityContextHolder.getContext().setAuthentication(authentication);   //    "이 요청은 n번 회원" 기록
        }

        filterChain.doFilter(request, response);   // 3. 다음 단계로 넘김 (막을지는 SecurityConfig 규칙이 결정)
    }

    // "Authorization: Bearer 토큰" 헤더에서 토큰 부분만 꺼내기
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");          // 헤더 값 가져오기
        if (header != null && header.startsWith("Bearer ")) {        // JWT 방식으로 보냈는지 확인
            return header.substring(7);                              // 앞의 "Bearer "(7글자) 잘라내고 토큰만
        }
        return null;                                                 // 헤더가 없거나 형식이 다르면 null
    }
}