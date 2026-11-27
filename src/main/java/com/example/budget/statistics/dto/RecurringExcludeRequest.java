package com.example.budget.statistics.dto;   // statistics/dto 패키지

import jakarta.validation.constraints.NotBlank;   // 빈칸 금지
import jakarta.validation.constraints.NotNull;    // null 금지
import jakarta.validation.constraints.Size;       // 길이 제한

// "아니에요"를 눌렀을 때 화면이 보내는 값: 어느 묶음을 제외할지 (카테고리 id + 이름)
public record RecurringExcludeRequest(

        @NotNull(message = "카테고리 정보가 없습니다.")
        Long categoryId,                                        // 정기 지출 결과의 categoryId 그대로

        @NotBlank(message = "이름 정보가 없습니다.")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        String name                                             // 정기 지출 결과의 name 그대로 ("점심")
) {
}
// 화면에서 이렇게 보냄: { "categoryId": 7, "name": "점심" }