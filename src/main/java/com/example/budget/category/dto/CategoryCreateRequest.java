package com.example.budget.category.dto;   // category/dto 패키지

import com.example.budget.category.TransactionType;   // 수입/지출 enum
import jakarta.validation.constraints.NotBlank;       // 빈칸 금지
import jakarta.validation.constraints.NotNull;        // null 금지 (글자가 아닌 값용)
import jakarta.validation.constraints.Size;           // 길이 제한

// 카테고리 추가할 때 화면이 보내는 데이터 상자
public record CategoryCreateRequest(

        @NotBlank(message = "카테고리 이름을 입력해 주세요.")             // 비어 있으면 안 됨
        @Size(max = 20, message = "카테고리 이름은 20자 이하로 입력해 주세요.") // DB 컬럼 길이(20)에 맞춤
        String name,                                                   // 칸 1: 카테고리 이름

        @NotNull(message = "수입/지출 구분을 선택해 주세요.")              // 꼭 있어야 함
        TransactionType type                                           // 칸 2: INCOME 또는 EXPENSE
) {
}
// 화면에서 이렇게 보냄: { "name": "카페", "type": "EXPENSE" }
// type에 "ABC"처럼 없는 값을 보내면 JSON 변환 실패 → 400 INVALID_INPUT