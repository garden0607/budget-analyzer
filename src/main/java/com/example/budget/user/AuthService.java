package com.example.budget.user;   // user 패키지

import com.example.budget.category.Category;                  // 카테고리 엔티티
import com.example.budget.category.CategoryRepository;        // 카테고리 저장소
import com.example.budget.category.TransactionType;           // 수입/지출 구분 enum
import com.example.budget.global.exception.BusinessException; // 우리가 만든 예외
import com.example.budget.global.exception.ErrorCode;         // 오류 목록표
import com.example.budget.global.security.JwtProvider;        // ★ 추가: 토큰 발급 도구 (4일차)
import com.example.budget.user.dto.LoginRequest;              // ★ 추가: 로그인 요청 상자
import com.example.budget.user.dto.SignupRequest;             // 회원가입 요청 상자
import com.example.budget.user.dto.TokenResponse;             // ★ 추가: 토큰 응답 상자
import lombok.RequiredArgsConstructor;                        // final 필드 생성자 자동 생성
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 암호화 도구
import org.springframework.stereotype.Service;                // Service 표시
import org.springframework.transaction.annotation.Transactional;     // 트랜잭션

import java.util.ArrayList;
import java.util.List;

@Service                        // 스프링이 Service로 등록·관리
@RequiredArgsConstructor        // final 필드를 받는 생성자 자동 생성
@Transactional(readOnly = true) // 기본은 읽기 전용 트랜잭션
public class AuthService {

    private final UserRepository userRepository;         // 회원 DB 작업
    private final CategoryRepository categoryRepository; // 카테고리 DB 작업
    private final PasswordEncoder passwordEncoder;       // BCrypt 암호화·비교
    private final JwtProvider jwtProvider;               // ★ 추가: JWT 발급

    // 가입 시 자동으로 만들 기본 카테고리 (설계서: 지출 6개, 수입 3개)
    private static final List<String> DEFAULT_EXPENSE = List.of("식비", "교통비", "주거·통신", "생활용품", "문화·여가", "기타");
    private static final List<String> DEFAULT_INCOME = List.of("급여", "용돈", "기타");

    // ───────────── 회원가입 (FR-01) ─────────────
    @Transactional  // 저장하므로 읽기 전용 해제, 실패 시 전부 취소
    public void signup(SignupRequest request) {

        // 1. 비밀번호와 비밀번호 확인 비교
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);  // 400
        }

        // 2. 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);    // 409
        }

        // 3. 비밀번호 암호화 후 회원 저장
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))  // BCrypt 암호화
                .nickname(request.nickname())
                .build();
        userRepository.save(user);

        // 4. 기본 카테고리 9개 생성
        createDefaultCategories(user);
    }

    // ───────────── 로그인 (FR-02) ─────────────  ★ 추가
    public TokenResponse login(LoginRequest request) {   // 조회만 하니까 읽기 전용 그대로 사용

        // 1. 이메일로 회원 찾기 (2일차에 만든 findByEmail → Optional로 돌려줌)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED)); // 없으면 401

        // 2. 입력한 비밀번호와 DB의 암호화된 비밀번호 비교
        //    matches(입력값, 저장된값): 입력값을 같은 방식으로 암호화해서 비교해 줌
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);                   // 틀리면 401
        }

        // 3. 회원 id를 담은 JWT 발급 (4일차 JwtProvider.createToken)
        String token = jwtProvider.createToken(user.getId());

        // 4. 토큰과 닉네임을 응답 상자에 담아 돌려줌
        return new TokenResponse(token, user.getNickname());
    }

    // ───────────── 기본 카테고리 생성 ─────────────
    private void createDefaultCategories(User user) {
        List<Category> categories = new ArrayList<>();   // 만들 카테고리 목록

        for (String name : DEFAULT_EXPENSE) {             // 지출 6개
            categories.add(Category.builder()
                    .user(user)
                    .name(name)
                    .type(TransactionType.EXPENSE)
                    .isDefault(true)
                    .build());
        }
        for (String name : DEFAULT_INCOME) {              // 수입 3개
            categories.add(Category.builder()
                    .user(user)
                    .name(name)
                    .type(TransactionType.INCOME)
                    .isDefault(true)
                    .build());
        }
        categoryRepository.saveAll(categories);           // 한 번에 저장
    }
}