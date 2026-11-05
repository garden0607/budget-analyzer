package com.example.budget.category.dto;   // category/dto 패키지

import com.example.budget.category.Category;          // 카테고리 엔티티
import com.example.budget.category.TransactionType;   // 수입/지출 enum

// 서버가 화면에 돌려주는 카테고리 정보 상자
public record CategoryResponse(
        Long id,                 // 카테고리 id (수정·삭제할 때 화면이 이 값을 씀)
        String name,             // 이름
        TransactionType type,    // 구분
        boolean isDefault        // 기본 카테고리 여부 (화면에 "기본" 표시용)
) {

    // 엔티티 → 응답 상자로 바꾸는 메서드
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),          // 엔티티의 id
                category.getName(),        // 엔티티의 이름
                category.getType(),        // 엔티티의 구분
                category.isDefault());     // boolean은 Lombok이 getIsDefault()가 아니라 isDefault()로 만들어 줌
    }
}
// 화면에는 이렇게 전달됨: { "id": 1, "name": "식비", "type": "EXPENSE", "isDefault": true }