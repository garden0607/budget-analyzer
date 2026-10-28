package com.example.budget.transaction;

import com.example.budget.category.Category;
import com.example.budget.global.BaseTimeEntity;
import com.example.budget.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_tx_user_date", columnList = "user_id, transaction_date"),
                @Index(name = "idx_tx_category_date", columnList = "category_id, transaction_date")
        })
public class Transaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(length = 100)
    private String memo;

    @Builder
    private Transaction(User user, Category category, int amount, LocalDate transactionDate, String memo) {
        this.user = user;
        this.category = category;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.memo = memo;
    }

    public void update(Category category, int amount, LocalDate transactionDate, String memo) {
        this.category = category;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.memo = memo;
    }
}