package com.example.budget.statistics.dto;   // statistics/dto 패키지

import java.util.List;   // 여러 개 결과

// 전월 대비 증감 (막대 차트 + 표)
public record ComparisonResponse(
        String month,               // 이번 달 ("2026-11")
        String previousMonth,       // 지난달 ("2026-10")
        List<Item> items            // 카테고리별 비교 줄
) {
    // 카테고리 한 줄
    public record Item(
            Long categoryId,        // 카테고리 id
            String categoryName,    // 카테고리 이름
            long thisMonth,         // 이번 달 지출
            long lastMonth,         // 지난달 지출
            long diff,              // 증감액 = 이번 달 - 지난달 (줄었으면 음수)
            Double changeRate       // 증감률 % (지난달 0원이면 null → 화면에 "신규")
    ) {
    }
}
// { "month": "2026-11", "previousMonth": "2026-10",
//   "items": [ { "categoryName": "식비", "thisMonth": 9000, "lastMonth": 12000, "diff": -3000, "changeRate": -25.0 }, ... ] }