package com.example.budget.statistics.dto;   // statistics/dto 패키지

import java.util.List;   // 여러 개 결과

// 카테고리별 지출 비율 (원형 차트)
public record CategoryRatioResponse(
        String month,               // 조회한 월
        long totalExpense,          // 전체 지출 (비율의 분모)
        List<Item> items            // 카테고리별 줄 목록 (금액 큰 순)
) {
    // 카테고리 한 줄 (record 안에 record를 넣어서 한 파일로 관리)
    public record Item(
            Long categoryId,        // 카테고리 id
            String categoryName,    // 카테고리 이름 ("식비")
            long amount,            // 이 카테고리 지출
            double ratio            // 비율 % (소수점 첫째 자리, 예: 50.0)
    ) {
    }
}
// { "month": "2026-11", "totalExpense": 18000,
//   "items": [ { "categoryName": "식비", "amount": 9000, "ratio": 50.0 }, ... ] }