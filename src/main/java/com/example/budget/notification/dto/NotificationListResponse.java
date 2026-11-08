package com.example.budget.notification.dto;   // notification/dto 패키지

import java.util.List;   // 여러 개 결과

// 알림 목록 + 안 읽은 개수 (상단 🔔 드롭다운용)
public record NotificationListResponse(
        long unreadCount,                          // 안 읽은 알림 개수 → 🔔 옆 빨간 숫자
        List<NotificationResponse> notifications   // 알림 목록 (최신순)
) {
}
// 화면에는 이렇게 전달됨: { "unreadCount": 3, "notifications": [ {...}, {...}, {...} ] }