package com.example.budget.budget;   // budget 패키지

// 예산 상태: 사용률에 따라 바뀜 (10주차 핵심 로직에서 판정)
public enum BudgetStatus {
    NORMAL,     // 정상 (사용률 80% 미만)
    WARNING,    // 경고 (사용률 80% 이상 100% 미만)
    EXCEEDED    // 초과 (사용률 100% 이상)
}