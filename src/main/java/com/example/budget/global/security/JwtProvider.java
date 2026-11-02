package com.example.budget.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;      // 서명용 비밀키
    private final long expiration;    // 유효 시간(밀리초)

    // application.yml의 jwt.secret, jwt.expiration 값을 읽어옴
    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    // 발급: 회원 id를 담은 토큰 생성
    public String createToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))                  // 내용: 회원 id
                .issuedAt(now)                                    // 내용: 발급 시간
                .expiration(new Date(now.getTime() + expiration)) // 내용: 만료 시간
                .signWith(key)                                    // 비밀키로 서명
                .compact();                                       // 문자열로 완성
    }

    // 검증: 서명이 맞고 만료되지 않았는지 확인
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;   // 위조, 만료, 형식 오류 모두 여기로
        }
    }

    // 토큰에서 회원 id 꺼내기
    public Long getUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}