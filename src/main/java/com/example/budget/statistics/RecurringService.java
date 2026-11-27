package com.example.budget.statistics;   // statistics 패키지

import com.example.budget.statistics.dto.ExpenseRecord;      // 알고리즘 입력
import com.example.budget.statistics.dto.RecurringResponse;  // 알고리즘 결과
import com.example.budget.transaction.TransactionRepository; // 거래 DB 작업
import lombok.RequiredArgsConstructor;                       // final 필드 생성자 자동 생성
import org.springframework.stereotype.Service;               // Service 표시
import org.springframework.transaction.annotation.Transactional; // 트랜잭션

import java.time.LocalDate;   // 날짜
import java.time.ZoneId;      // 시간대
import java.util.List;        // 목록

// 정기 지출 탐지: DB에서 거래를 꺼내 알고리즘(RecurringDetector)에 넘기고 결과를 돌려줌
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 조회만 함
public class RecurringService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");   // "오늘"은 한국 시간 기준
    private static final int LOOKBACK_MONTHS = 12;                  // 최근 12개월 거래만 봄

    private final TransactionRepository transactionRepository;      // 거래 조회
    private final RecurringDetector recurringDetector;              // 알고리즘 (스프링이 넣어 줌)

    public RecurringResponse getRecurring(Long userId) {
        LocalDate today = LocalDate.now(KOREA);                      // 오늘

        List<ExpenseRecord> records = transactionRepository
                .findExpensesBetween(userId, today.minusMonths(LOOKBACK_MONTHS), today) // 최근 12개월 지출
                .stream()
                .map(t -> new ExpenseRecord(                         // 엔티티 → 알고리즘 입력으로 변환
                        t.getCategory().getId(),
                        t.getCategory().getName(),
                        t.getAmount(),
                        t.getTransactionDate(),
                        t.getMemo()))
                .toList();

        List<RecurringResponse.Item> items = recurringDetector.detect(records, today); // ★ 알고리즘 실행

        long monthlyTotal = items.stream()
                .mapToLong(RecurringResponse.Item::amount)            // 각 정기 지출의 최근 금액을
                .sum();                                               // 모두 더해 "매달 나가는 돈"

        return new RecurringResponse(items, monthlyTotal);
    }
}