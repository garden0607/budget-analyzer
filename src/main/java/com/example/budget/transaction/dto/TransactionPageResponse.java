package com.example.budget.transaction.dto;   // transaction/dto 패키지

import com.example.budget.transaction.Transaction;   // 거래 엔티티
import org.springframework.data.domain.Page;         // 페이지 결과 (목록 + 전체 건수 등)

import java.time.LocalDate;   // 날짜 타입
import java.util.List;        // 여러 개 결과

// 거래 내역 조회 결과를 화면에 돌려주는 상자
public record TransactionPageResponse(
        LocalDate startDate,                     // 실제로 적용한 시작일 (안 보냈으면 이번 달 1일)
        LocalDate endDate,                       // 실제로 적용한 종료일 (안 보냈으면 이번 달 말일)
        List<TransactionResponse> transactions,  // 이번 페이지의 거래 목록 (최대 20건)
        long totalCount,                         // 조건에 맞는 전체 거래 수 (예: 45)
        int page,                                // 지금 페이지 번호 (0부터 시작)
        int totalPages,                          // 전체 페이지 수 (45건이면 3)
        long incomeTotal,                        // 조회 기간 수입 합계 (페이지 상관없이 전체)
        long expenseTotal                        // 조회 기간 지출 합계 (페이지 상관없이 전체)
) {

    // 검색 결과(Page) + 합계 → 응답 상자로 바꾸는 메서드
    public static TransactionPageResponse of(LocalDate startDate, LocalDate endDate,
                                             Page<Transaction> result,
                                             long incomeTotal, long expenseTotal) {
        return new TransactionPageResponse(
                startDate,                                     // 적용한 시작일
                endDate,                                       // 적용한 종료일
                result.getContent().stream()                   // 이번 페이지 거래들을 하나씩 꺼내서
                        .map(TransactionResponse::from)        // 엔티티 → 응답 상자로 바꾸고
                        .toList(),                             // 목록으로 모음
                result.getTotalElements(),                     // 전체 건수 (countQuery 결과)
                result.getNumber(),                            // 지금 페이지 번호
                result.getTotalPages(),                        // 전체 페이지 수
                incomeTotal,                                   // 수입 합계
                expenseTotal);                                 // 지출 합계
    }
}
// 화면에는 이렇게 전달됨:
// { "startDate": "2026-11-01", "endDate": "2026-11-30",
//   "transactions": [ {...}, {...} ], "totalCount": 45, "page": 0, "totalPages": 3,
//   "incomeTotal": 2500000, "expenseTotal": 830000 }