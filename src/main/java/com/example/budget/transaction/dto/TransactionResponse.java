package com.example.budget.transaction.dto;   // transaction/dto 패키지

import com.example.budget.category.TransactionType;   // 수입/지출 enum
import com.example.budget.transaction.Transaction;    // 거래 엔티티

import java.time.LocalDate;   // 날짜 타입

// 서버가 화면에 돌려주는 거래 정보 상자
public record TransactionResponse(
        Long id,                  // 거래 id (수정·삭제할 때 씀)
        Long categoryId,          // 카테고리 id (수정 모달에서 선택된 카테고리 표시용)
        String categoryName,      // 카테고리 이름 (목록에 "식비" 표시용)
        TransactionType type,     // 수입/지출 (카테고리에서 가져옴, 목록에 색 구분용)
        int amount,               // 금액
        LocalDate transactionDate,// 거래일
        String memo               // 메모
) {

    // 엔티티 → 응답 상자로 바꾸는 메서드
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),                       // 거래 id
                transaction.getCategory().getId(),         // 거래의 카테고리 id
                transaction.getCategory().getName(),       // 거래의 카테고리 이름
                transaction.getCategory().getType(),       // 수입/지출은 카테고리를 따름 ← 거래에 type 칸이 없는 이유
                transaction.getAmount(),                   // 금액
                transaction.getTransactionDate(),          // 거래일
                transaction.getMemo());                    // 메모
    }
}
// 화면에는 이렇게 전달됨:
// { "id": 1, "categoryId": 1, "categoryName": "식비", "type": "EXPENSE", "amount": 9000, "transactionDate": "2026-11-05", "memo": "점심" }