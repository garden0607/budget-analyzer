package com.example.budget.budget.dto;   // budget/dto 패키지

import jakarta.validation.constraints.Max;      // 최댓값 제한
import jakarta.validation.constraints.Min;      // 최솟값 제한
import jakarta.validation.constraints.NotBlank; // 빈칸 금지 (글자용)
import jakarta.validation.constraints.NotNull;  // null 금지
import jakarta.validation.constraints.Pattern;  // 글자 모양 검사

// 예산 설정할 때 화면이 보내는 데이터 상자 (새로 만들기·수정 둘 다 이 상자로)
public record BudgetRequest(

        @NotNull(message = "카테고리를 선택해 주세요.")                       // 꼭 있어야 함
        Long categoryId,                                                    // 예산을 정할 카테고리 (지출 카테고리만 가능, Service에서 확인)

        @NotBlank(message = "적용 월을 입력해 주세요.")                        // 비어 있으면 안 됨
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",                       // "2026-11"처럼 연도 4자리-월 01~12
                message = "적용 월은 2026-11 형식으로 입력해 주세요.")
        String budgetMonth,                                                 // 적용 월 (DB에 CHAR(7)로 저장)

        @NotNull(message = "예산 금액을 입력해 주세요.")                       // 꼭 있어야 함
        @Min(value = 1, message = "예산은 1원 이상이어야 합니다.")              // 1원 이상
        @Max(value = 10_000_000, message = "예산은 1,000만 원 이하로 입력해 주세요.") // 1,000만 원 이하 (설계서)
        Integer amount                                                      // 예산 금액 (Integer: 안 보내면 null로 받아서 @NotNull로 잡기)
) {
}
// 화면에서 이렇게 보냄: { "categoryId": 1, "budgetMonth": "2026-11", "amount": 100000 }