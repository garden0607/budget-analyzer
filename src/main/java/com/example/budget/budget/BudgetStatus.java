package com.example.budget.budget;   // budget 패키지

// 예산 상태: 사용률에 따라 정해짐 (순서가 곧 위험도: NORMAL < WARNING < EXCEEDED)
public enum BudgetStatus {
    NORMAL,     // 정상 (사용률 80% 미만)
    WARNING,    // 경고 (사용률 80% 이상 100% 미만)
    EXCEEDED;   // 초과 (사용률 100% 이상) ← 아래에 메서드가 있어서 마지막 항목 뒤에 세미콜론(;)

    private static final int WARNING_RATE = 80;    // 경고 기준 (%)
    private static final int EXCEEDED_RATE = 100;  // 초과 기준 (%)

    // 사용률(%)을 보고 상태를 정함
    public static BudgetStatus of(int usageRate) {
        if (usageRate >= EXCEEDED_RATE) {   // 100% 이상
            return EXCEEDED;
        }
        if (usageRate >= WARNING_RATE) {    // 80% 이상
            return WARNING;
        }
        return NORMAL;                      // 그 밖
    }

    // 이 상태가 다른 상태보다 더 위험한지 (알림을 보낼지 판단할 때 사용)
    public boolean isMoreSevereThan(BudgetStatus other) {
        return this.ordinal() > other.ordinal();   // ordinal(): 선언 순서 번호 (NORMAL 0, WARNING 1, EXCEEDED 2)
    }
}
// 사용 예:
// BudgetStatus.of(85)                          → WARNING
// WARNING.isMoreSevereThan(NORMAL)             → true  (위험해짐 → 알림)
// WARNING.isMoreSevereThan(EXCEEDED)           → false (덜 위험해짐 → 알림 없음)