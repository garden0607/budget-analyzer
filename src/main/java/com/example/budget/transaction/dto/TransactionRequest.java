package com.example.budget.transaction.dto;   // transaction/dto 패키지

import jakarta.validation.constraints.Max;      // 최댓값 제한
import jakarta.validation.constraints.Min;      // 최솟값 제한
import jakarta.validation.constraints.NotNull;  // null 금지
import jakarta.validation.constraints.Size;     // 길이 제한

import java.time.LocalDate;   // 날짜 타입 (시간 없음)

// 거래 등록·수정할 때 화면이 보내는 데이터 상자 (둘이 보내는 값이 같아서 같이 씀)
public record TransactionRequest(

        @NotNull(message = "카테고리를 선택해 주세요.")                    // 꼭 있어야 함
        Long categoryId,                                                 // 카테고리 id (수입/지출 구분은 이 카테고리를 따름)

        @NotNull(message = "금액을 입력해 주세요.")                        // 꼭 있어야 함
        @Min(value = 1, message = "금액은 1원 이상이어야 합니다.")           // 1원 이상
        @Max(value = 10_000_000, message = "금액은 1,000만 원 이하로 입력해 주세요.") // 1,000만 원 이하 (10_000_000 = 10000000, _는 읽기 쉽게 끊은 것)
        Integer amount,                                                  // 금액 (int가 아니라 Integer: 안 보냈을 때 null로 받아서 @NotNull로 잡으려고)

        @NotNull(message = "날짜를 선택해 주세요.")                        // 꼭 있어야 함
        LocalDate transactionDate,                                       // 거래일 ("2026-11-05" 형식으로 받음, 1년 이내인지는 Service에서 검사)

        @Size(max = 100, message = "메모는 100자 이하로 입력해 주세요.")     // 최대 100자
        String memo                                                      // 메모 (선택, 안 보내도 됨)
) {
}
// 화면에서 이렇게 보냄: { "categoryId": 1, "amount": 9000, "transactionDate": "2026-11-05", "memo": "점심" }