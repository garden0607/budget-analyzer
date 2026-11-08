package com.example.budget.budget.dto;   // budget/dto 패키지

import java.util.List;   // 여러 개 결과

// 예산 사용 현황 전체 응답 (예산 화면, 대시보드용)
public record BudgetStatusResponse(
        String month,                   // 조회한 월 ("2026-11")
        List<BudgetStatusItem> items,   // 지출 카테고리별 줄 목록
        long totalBudget,               // 예산 합계 (예산 있는 카테고리만)
        long totalSpent,                // 사용 합계 (예산 있는 카테고리만 → 예산과 비교하려고)
        long totalRemaining,            // 잔여 합계 = 예산 합계 - 사용 합계
        Integer totalUsageRate          // 전체 사용률 (예산이 하나도 없으면 null)
) {
}