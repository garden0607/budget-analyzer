package com.example.budget.budget.dto;   // budget/dto 패키지

import com.example.budget.budget.Budget;         // 예산 엔티티
import com.example.budget.budget.BudgetStatus;   // 예산 상태 enum

// 예산 설정 후 서버가 돌려주는 데이터 상자
public record BudgetResponse(
        Long id,               // 예산 id
        Long categoryId,       // 카테고리 id
        String categoryName,   // 카테고리 이름 ("식비")
        String budgetMonth,    // 적용 월 ("2026-11")
        int amount,            // 예산 금액
        BudgetStatus status    // 현재 상태 (NORMAL / WARNING / EXCEEDED) → 설정하자마자 초과일 수도 있음
) {

    // 엔티티 → 응답 상자로 바꾸는 메서드
    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getId(),                       // 예산 id
                budget.getCategory().getId(),         // 카테고리 id
                budget.getCategory().getName(),       // 카테고리 이름
                budget.getBudgetMonth(),              // 적용 월
                budget.getAmount(),                   // 예산 금액
                budget.getStatus());                  // 현재 상태
    }
}
// 화면에는 이렇게 전달됨:
// { "id": 1, "categoryId": 1, "categoryName": "식비", "budgetMonth": "2026-11", "amount": 100000, "status": "NORMAL" }