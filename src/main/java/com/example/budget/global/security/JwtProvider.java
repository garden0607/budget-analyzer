package com.example.budget.global.security;   // global/security 패키지

import io.jsonwebtoken.Claims;                              // 토큰의 내용(payload) 부분
import io.jsonwebtoken.JwtException;                        // 토큰 검증 실패 시 나는 예외
import io.jsonwebtoken.Jwts;                                // 토큰 만들기·읽기 시작점
import io.jsonwebtoken.security.Keys;                       // 비밀키 객체 만드는 도구
import org.springframework.beans.factory.annotation.Value;  // application.yml 값 읽기 (Lombok @Value 아님)
import org.springframework.stereotype.Component;            // 스프링 관리 대상으로 등록

import javax.crypto.SecretKey;              // 서명용 비밀키 타입
import java.nio.charset.StandardCharsets;   // 글자 → 바이트 변환 방식(UTF-8)
import java.util.Date;                      // 발급·만료 시간

// JWT 발급, 검증, 회원 id 꺼내기를 맡는 클래스
@Component   // 스프링이 하나 만들어 관리 → AuthService, SecurityConfig에서 주입받아 씀
public class JwtProvider {

    private final SecretKey key;     // 서명용 비밀키
    private final long expiration;   // 토큰 유효 시간 (밀리초, 3600000 = 1시간)

    public JwtProvider(@Value("${jwt.secret}") String secret,          // yml의 jwt.secret 값
                       @Value("${jwt.expiration}") long expiration) {  // yml의 jwt.expiration 값
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // 글자 비밀키 → 서명용 키 객체 (짧으면 오류)
        this.expiration = expiration;                                         // 유효 시간 저장
    }

    // 발급: 로그인 성공 시 AuthService가 호출
    public String createToken(Long userId) {
        Date now = new Date();                                    // 지금 시간
        return Jwts.builder()                                     // 토큰 만들기 시작
                .subject(String.valueOf(userId))                  // 내용: 토큰 주인 = 회원 id
                .issuedAt(now)                                    // 내용: 발급 시간
                .expiration(new Date(now.getTime() + expiration)) // 내용: 만료 시간 (지금 + 1시간)
                .signWith(key)                                    // 비밀키로 서명 (키 길이 보고 HS512 자동 선택)
                .compact();                                       // "헤더.내용.서명" 문자열로 완성
    }

    // 검증: 요청마다 필터가 호출
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);   // 서명 확인 + 만료 확인을 한 번에
            return true;                                                      // 문제없으면 통과
        } catch (JwtException | IllegalArgumentException e) {                // 위조, 만료, 형식 오류, 빈 값
            return false;                                                     // 하나라도 걸리면 실패
        }
    }

    // 토큰에서 회원 id 꺼내기: 검증 통과 후 필터가 호출
    public Long getUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()   // 같은 키로 읽기 준비
                .parseSignedClaims(token)                       // 토큰 해석
                .getPayload();                                  // 내용 부분 꺼내기
        return Long.valueOf(claims.getSubject());               // subject("1") → 숫자 1로 변환
    }
}