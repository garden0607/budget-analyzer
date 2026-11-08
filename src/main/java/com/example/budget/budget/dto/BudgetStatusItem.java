package com.example.budget.budget.dto;   // budget/dto 패키지

import com.example.budget.budget.BudgetStatus;   // 예산 상태 enum

// 예산 사용 현황의 카테고리 한 줄
public record BudgetStatusItem(
        Long categoryId,        // 카테고리 id
        String categoryName,    // 카테고리 이름 ("식비")
        Integer budget,         // 예산 금액 (예산이 없으면 null)
        long spent,             // 사용 금액 (그 달 지출 합계, 없으면 0)
        Long remaining,         // 잔여 금액 = 예산 - 사용 (초과하면 음수, 예산 없으면 null)
        Integer usageRate,      // 사용률 % (예산 없으면 null)
        BudgetStatus status     // 상태 (예산 없으면 null)
) {
}
// 예산 있음: { "categoryName": "교통비", "budget": 8000, "spent": 9000, "remaining": -1000, "usageRate": 112, "status": "EXCEEDED" }
// 예산 없음: { "categoryName": "주거·통신", "budget": null, "spent": 0, "remaining": null, "usageRate": null, "status": null }