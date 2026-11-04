package com.example.budget.user;   // user 패키지

import com.example.budget.user.dto.LoginRequest;   // 로그인 요청 상자
import com.example.budget.user.dto.SignupRequest;  // 회원가입 요청 상자
import com.example.budget.user.dto.TokenResponse;  // 토큰 응답 상자
import jakarta.validation.Valid;                   // DTO의 검증 규칙을 실행시키는 어노테이션
import lombok.RequiredArgsConstructor;             // final 필드 생성자 자동 생성
import org.springframework.http.HttpStatus;        // HTTP 상태 코드 목록 (200, 201 ...)
import org.springframework.http.ResponseEntity;    // 상태 코드 + 내용을 함께 담는 응답
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController               // 이 클래스는 API 창구. 돌려주는 값은 자동으로 JSON이 됨
@RequestMapping("/api/auth")  // 이 클래스의 모든 주소 앞에 /api/auth가 붙음
@RequiredArgsConstructor      // final 필드를 받는 생성자 자동 생성
public class AuthController {

    private final AuthService authService;   // 실제 처리는 Service에 맡김

    // 회원가입: POST /api/auth/signup
    @PostMapping("/signup")                               // POST 방식 + /signup 주소와 연결
    public ResponseEntity<Void> signup(
            @Valid                                        // SignupRequest의 검증 규칙(@NotBlank, @Pattern ...) 실행
            @RequestBody SignupRequest request) {         // 화면이 보낸 JSON 본문을 SignupRequest로 변환
        authService.signup(request);                      // Service에 가입 처리 맡김
        return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created, 내용 없이 응답
    }

    // 로그인: POST /api/auth/login
    @PostMapping("/login")                                // POST 방식 + /login 주소와 연결
    public ResponseEntity<TokenResponse> login(
            @Valid                                        // LoginRequest의 검증 규칙 실행
            @RequestBody LoginRequest request) {          // JSON 본문 → LoginRequest
        TokenResponse response = authService.login(request); // Service에 로그인 처리 맡기고 토큰 받기
        return ResponseEntity.ok(response);               // 200 OK + { accessToken, nickname }
    }
}