package com.example.budget.category.dto;   // category/dto 패키지

import jakarta.validation.constraints.NotBlank;   // 빈칸 금지
import jakarta.validation.constraints.Size;       // 길이 제한

// 카테고리 이름 수정할 때 화면이 보내는 데이터 상자
public record CategoryUpdateRequest(

        @NotBlank(message = "카테고리 이름을 입력해 주세요.")             // 비어 있으면 안 됨
        @Size(max = 20, message = "카테고리 이름은 20자 이하로 입력해 주세요.") // 최대 20자
        String name                                                    // 새 이름 (구분은 못 바꾸니까 이름만 받음)
) {
}
// 수정할 카테고리 id는 상자가 아니라 주소로 받음: PUT /api/categories/15