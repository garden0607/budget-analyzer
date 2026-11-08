package com.example.budget.transaction.dto;   // transaction/dto 패키지

// 카테고리별 지출 합계 한 줄을 담는 상자 (GROUP BY 결과용)
public record CategorySpending(
        Long categoryId,   // 어느 카테고리
        Long total         // 그 기간 지출 합계 (SUM 결과는 Long)
) {
}
// 예: CategorySpending(1, 9000) → 식비 9,000원