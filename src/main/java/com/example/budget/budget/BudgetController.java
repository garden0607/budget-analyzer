package com.example.budget.budget;   // budget 패키지

import com.example.budget.budget.dto.BudgetRequest;    // 예산 설정 요청 상자
import com.example.budget.budget.dto.BudgetResponse;   // 예산 응답 상자
import jakarta.validation.Valid;                       // DTO 검증 실행
import lombok.RequiredArgsConstructor;                 // final 필드 생성자 자동 생성
import org.springframework.http.ResponseEntity;        // 상태 코드 + 내용 응답
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 토큰의 회원 id 꺼내기
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController                 // API 창구, 돌려주는 값은 자동으로 JSON
@RequestMapping("/api/budgets") // 이 클래스의 모든 주소 앞에 /api/budgets
@RequiredArgsConstructor        // final 필드 생성자 자동 생성
public class BudgetController {

    private final BudgetService budgetService;   // 실제 처리는 Service에 맡김

    // 예산 설정: PUT /api/budgets (없으면 생성, 있으면 금액 수정)
    @PutMapping
    public ResponseEntity<BudgetResponse> setBudget(
            @AuthenticationPrincipal Long userId,                  // 토큰에서 꺼낸 회원 id
            @Valid @RequestBody BudgetRequest request) {           // JSON → 상자 + 검증 (월 형식, 금액 1~1,000만 원)
        return ResponseEntity.ok(budgetService.setBudget(userId, request));   // 200 + 설정된 예산과 상태
    }
}