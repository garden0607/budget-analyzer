package com.example.budget.notification.dto;   // notification/dto 패키지

import com.example.budget.notification.Notification;       // 알림 엔티티
import com.example.budget.notification.NotificationType;   // 알림 종류 enum

import java.time.LocalDateTime;   // 날짜 + 시간

// 알림 한 건을 화면에 돌려주는 상자
public record NotificationResponse(
        Long id,                  // 알림 id (읽음 처리할 때 씀)
        NotificationType type,    // WARNING / EXCEEDED (화면에서 노랑·빨강 구분)
        String categoryName,      // 카테고리 이름 (알림 생길 때 복사해 둔 것)
        String budgetMonth,       // 어느 달 예산 ("2026-11")
        int usageRate,            // 알림 생긴 순간의 사용률
        String message,           // 표시 문구
        boolean isRead,           // 읽었는지 (안 읽은 건 굵게 표시)
        LocalDateTime createdAt   // 알림 생긴 시각 ("3분 전" 같은 표시용)
) {

    // 엔티티 → 응답 상자
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getCategoryName(),
                n.getBudgetMonth(),
                n.getUsageRate(),
                n.getMessage(),
                n.isRead(),          // boolean이라 getIsRead()가 아니라 isRead()
                n.getCreatedAt());   // BaseCreatedEntity에서 물려받은 값
    }
}