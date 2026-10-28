package com.example.budget.notification;

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
@Table(name = "notifications",
        indexes = @Index(name = "idx_noti_user_read", columnList = "user_id, is_read"))
public class Notification extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationType type;

    @Column(nullable = false, length = 20)
    private String categoryName;

    @Column(nullable = false, length = 7, columnDefinition = "CHAR(7)")
    private String budgetMonth;

    @Column(nullable = false)
    private int usageRate;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @Builder
    private Notification(User user, NotificationType type, String categoryName,
                         String budgetMonth, int usageRate, String message) {
        this.user = user;
        this.type = type;
        this.categoryName = categoryName;
        this.budgetMonth = budgetMonth;
        this.usageRate = usageRate;
        this.message = message;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}