package com.example.budget.budget;

import com.example.budget.category.Category;
import com.example.budget.global.BaseTimeEntity;
import com.example.budget.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "budgets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "budget_month"}))
public class Budget extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 7, columnDefinition = "CHAR(7)")
    private String budgetMonth;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BudgetStatus status = BudgetStatus.NORMAL;

    @Builder
    private Budget(User user, Category category, String budgetMonth, int amount) {
        this.user = user;
        this.category = category;
        this.budgetMonth = budgetMonth;
        this.amount = amount;
        this.status = BudgetStatus.NORMAL;
    }

    public void changeAmount(int amount) {
        this.amount = amount;
    }

    public void changeStatus(BudgetStatus status) {
        this.status = status;
    }
}