package com.example.budget.statistics.dto;   // statistics/dto 패키지

// 월별 수입·지출 합계 (통계 화면 위쪽 카드 3개, 대시보드)
public record SummaryResponse(
        String month,     // 조회한 월 ("2026-11")
        long income,      // 수입 합계
        long expense,     // 지출 합계
        long balance      // 잔액 = 수입 - 지출 (지출이 더 많으면 음수)
) {
}
// 화면에는 이렇게 전달됨: { "month": "2026-11", "income": 2500000, "expense": 18000, "balance": 2482000 }