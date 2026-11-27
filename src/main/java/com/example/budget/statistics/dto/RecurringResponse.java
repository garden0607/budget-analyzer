package com.example.budget.statistics.dto;   // statistics/dto 패키지

import java.util.List;   // 여러 개 결과

// 정기 지출 탐지 결과 (통계 화면의 "정기 지출로 보입니다" 카드)
public record RecurringResponse(
        List<Item> items,      // 정기 지출로 판단된 항목들 (금액 큰 순)
        long monthlyTotal      // 매달 나가는 정기 지출 합계 (각 항목 최근 금액의 합)
) {
    // 정기 지출 한 건 = 판단 이유까지 담은 한 줄
    public record Item(
            Long categoryId,          // 카테고리 id → [아니에요] 제외 요청에 사용
            String name,              // 이름: 최근 거래 메모, 없으면 카테고리 이름 ("넷플릭스")
            String categoryName,      // 카테고리 이름 ("구독")
            int amount,               // 최근 금액 (인상됐으면 오른 금액)
            int months,               // 연속 개월 수 (사슬 길이, 3 이상)
            int dayOfMonth,           // 결제일: 최근 거래의 날짜(일) → "매달 15일쯤"
            Integer previousAmount,   // 인상 전 금액 (금액이 안 바뀌었으면 null)
            String changedMonth       // 금액이 바뀐 달 "2026-10" (안 바뀌었으면 null)
    ) {
    }
}