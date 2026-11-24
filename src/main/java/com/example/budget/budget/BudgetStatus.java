package com.example.budget.budget;

public enum BudgetStatus {
    NORMAL,
    WARNING,
    EXCEEDED;

    private static final int WARNING_RATE = 80;
    private static final int EXCEEDED_RATE = 100;

    public static BudgetStatus of(int usageRate) {
        if (usageRate >= EXCEEDED_RATE) {
            return EXCEEDED;
        }
        if (usageRate >= WARNING_RATE) {
            return WARNING;
        }
        return NORMAL;
    }

    public boolean isMoreSevereThan(BudgetStatus other) {
        return this.ordinal() > other.ordinal();
    }
}