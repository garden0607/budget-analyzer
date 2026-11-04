package com.example.budget.notification;   // notification 패키지

// 알림 종류: 예산 상태가 바뀔 때 생성됨
public enum NotificationType {
    WARNING,    // 경고 알림 (사용률 80% 이상이 된 순간)
    EXCEEDED    // 초과 알림 (사용률 100% 이상이 된 순간)
}