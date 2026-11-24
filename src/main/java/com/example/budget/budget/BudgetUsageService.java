package com.example.budget.budget;

import com.example.budget.category.Category;
import com.example.budget.category.TransactionType;
import com.example.budget.notification.Notification;
import com.example.budget.notification.NotificationRepository;
import com.example.budget.notification.NotificationType;
import com.example.budget.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BudgetUsageService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void recalculate(Category category, YearMonth month) {

        if (category.getType() != TransactionType.EXPENSE) {
            return;
        }

        Optional<Budget> found = budgetRepository.findByCategoryIdAndBudgetMonth(
                category.getId(), month.toString());
        if (found.isEmpty()) {
            return;
        }
        Budget budget = found.get();

        long spent = transactionRepository.sumByCategoryAndPeriod(
                category.getId(), month.atDay(1), month.atEndOfMonth());

        int usageRate = calculateUsageRate(spent, budget.getAmount());
        BudgetStatus oldStatus = budget.getStatus();
        BudgetStatus newStatus = BudgetStatus.of(usageRate);

        if (newStatus.isMoreSevereThan(oldStatus)) {
            createNotification(budget, category, newStatus, usageRate);
        }

        budget.changeStatus(newStatus);
    }


    public static int calculateUsageRate(long spent, int budgetAmount) {
        long rate = spent * 100 / budgetAmount;
        return (int) Math.min(rate, Integer.MAX_VALUE);
    }

    private void createNotification(Budget budget, Category category, BudgetStatus status, int usageRate) {
        NotificationType type = (status == BudgetStatus.EXCEEDED)
                ? NotificationType.EXCEEDED
                : NotificationType.WARNING;

        String message = (type == NotificationType.EXCEEDED)
                ? String.format("%s 카테고리 예산을 초과했습니다 (%d%%)", category.getName(), usageRate)
                : String.format("%s 카테고리 예산의 %d%%를 사용했습니다", category.getName(), usageRate);

        Notification notification = Notification.builder()
                .user(budget.getUser())
                .type(type)
                .categoryName(category.getName())
                .budgetMonth(budget.getBudgetMonth())
                .usageRate(usageRate)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }
}