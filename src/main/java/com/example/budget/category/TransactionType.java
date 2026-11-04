package com.example.budget.category;   // category 패키지 (카테고리가 이 구분을 가짐)

// 거래 구분: 수입 / 지출 (거래는 카테고리의 구분을 따름)
public enum TransactionType {
    INCOME,    // 수입 (급여, 용돈 ...)
    EXPENSE    // 지출 (식비, 교통비 ...)
}