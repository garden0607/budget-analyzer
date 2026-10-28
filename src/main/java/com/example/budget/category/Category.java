package com.example.budget.category;

import com.example.budget.global.BaseCreatedEntity;
import com.example.budget.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type", "name"}))
public class Category extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false)
    private boolean isDefault;

    @Builder
    private Category(User user, String name, TransactionType type, boolean isDefault) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.isDefault = isDefault;
    }

    public void changeName(String name) {
        this.name = name;
    }
}