package com.example.budget.statistics.dto;   // statistics/dto 패키지

import java.time.LocalDate;   // 날짜 타입

// 정기 지출 탐지 알고리즘의 입력: 지출 거래 한 건 (알고리즘에 필요한 값만)
// 엔티티 대신 작은 record로 받아서, DB 없이 단위 테스트에서 바로 만들 수 있게 함
public record ExpenseRecord(
        Long categoryId,       // 카테고리 id → "같은 카테고리끼리만 묶기"에 사용
        String categoryName,   // 카테고리 이름 → 메모가 없을 때 결과 이름으로 사용
        int amount,            // 금액 → "금액 차이 30% 이내" 비교에 사용
        LocalDate date,        // 거래일 → "26~33일 간격" 비교에 사용
        String memo            // 메모 ("넷플릭스") → 결과 이름으로 사용 (없을 수 있음)
) {
}