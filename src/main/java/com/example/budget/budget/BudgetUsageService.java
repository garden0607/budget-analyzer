package com.example.budget.budget;   // budget 패키지

import com.example.budget.category.Category;                       // 카테고리 엔티티
import com.example.budget.category.TransactionType;                // 수입/지출 enum
import com.example.budget.notification.Notification;               // 알림 엔티티
import com.example.budget.notification.NotificationRepository;     // 알림 저장소
import com.example.budget.notification.NotificationType;           // 알림 종류 enum
import com.example.budget.transaction.TransactionRepository;       // 거래 저장소 (지출 합계)
import lombok.RequiredArgsConstructor;                             // final 필드 생성자 자동 생성
import org.springframework.stereotype.Service;                     // Service 표시
import org.springframework.transaction.annotation.Transactional;   // 트랜잭션

import java.time.YearMonth;   // 연·월 (2026-11)
import java.util.Optional;    // 있을 수도 없을 수도 있는 값

// 예산 사용률 재계산 + 알림 판정 (FR-11)
// 거래 등록·수정·삭제, 예산 설정 뒤에 자동으로 불림 (화면에서 직접 부르는 API 없음)
@Service                  // 스프링이 Service로 등록·관리
@RequiredArgsConstructor  // final 필드 생성자 자동 생성
public class BudgetUsageService {

    private final BudgetRepository budgetRepository;               // 예산 찾기, 상태 저장
    private final TransactionRepository transactionRepository;     // 그 달 지출 합계 구하기
    private final NotificationRepository notificationRepository;   // 알림 저장

    @Transactional   // 부른 쪽(거래·예산 Service)의 트랜잭션에 합류 → 거래 저장부터 알림까지 한 묶음
    public void recalculate(Category category, YearMonth month) {

        // 1. 지출 카테고리가 아니면 할 일 없음 (수입에는 예산이 없음)
        if (category.getType() != TransactionType.EXPENSE) {
            return;
        }

        // 2. 그 달 예산이 없으면 할 일 없음 (예산을 안 정한 카테고리)
        Optional<Budget> found = budgetRepository.findByCategoryIdAndBudgetMonth(
                category.getId(), month.toString());                 // YearMonth → "2026-11"
        if (found.isEmpty()) {
            return;
        }
        Budget budget = found.get();

        // 3. 그 달(1일 ~ 말일) 지출 합계
        long spent = transactionRepository.sumByCategoryAndPeriod(
                category.getId(), month.atDay(1), month.atEndOfMonth());

        // 4. 사용률 계산 → 새 상태 정하기
        int usageRate = calculateUsageRate(spent, budget.getAmount());   // 예: 85
        BudgetStatus oldStatus = budget.getStatus();                     // 저장돼 있던 이전 상태
        BudgetStatus newStatus = BudgetStatus.of(usageRate);            // 85 → WARNING

        // 5. 이전보다 위험해졌을 때만 알림 (정상→경고, 경고→초과, 정상→초과)
        if (newStatus.isMoreSevereThan(oldStatus)) {
            createNotification(budget, category, newStatus, usageRate);
        }

        // 6. 새 상태 저장 (변경 감지로 자동 UPDATE) → 다음 계산 때 "이전 상태"가 됨
        budget.changeStatus(newStatus);
    }

    // 사용률(%) = 지출 ÷ 예산 × 100 (소수점 버림)
    public static int calculateUsageRate(long spent, int budgetAmount) {
        long rate = spent * 100 / budgetAmount;               // 곱하기를 먼저 해야 정수 나눗셈에서 0이 안 됨
        return (int) Math.min(rate, Integer.MAX_VALUE);       // int 범위를 넘지 않게 (예산 1원에 큰 지출 같은 극단적인 경우)
    }

    // 알림 만들어 저장
    private void createNotification(Budget budget, Category category, BudgetStatus status, int usageRate) {
        NotificationType type = (status == BudgetStatus.EXCEEDED)    // 상태 → 알림 종류
                ? NotificationType.EXCEEDED
                : NotificationType.WARNING;

        String message = (type == NotificationType.EXCEEDED)         // 화면에 보여줄 문구 (설계서 예시)
                ? String.format("%s 카테고리 예산을 초과했습니다 (%d%%)", category.getName(), usageRate)
                : String.format("%s 카테고리 예산의 %d%%를 사용했습니다", category.getName(), usageRate);
        // %s: 글자 자리, %d: 숫자 자리, %%: 글자 "%" 그대로

        Notification notification = Notification.builder()
                .user(budget.getUser())                  // 알림 받을 회원 = 예산 주인
                .type(type)                              // WARNING / EXCEEDED
                .categoryName(category.getName())        // 카테고리 이름 복사 (나중에 이름이 바뀌어도 그대로)
                .budgetMonth(budget.getBudgetMonth())    // 어느 달 예산인지
                .usageRate(usageRate)                    // 알림 생긴 순간의 사용률
                .message(message)                        // 표시 문구
                .build();
        notificationRepository.save(notification);       // notifications 테이블에 INSERT
    }
}