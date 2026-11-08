package com.example.budget.notification;   // notification 패키지

import com.example.budget.global.exception.BusinessException;               // 우리가 만든 예외
import com.example.budget.global.exception.ErrorCode;                      // 오류 목록표
import com.example.budget.notification.dto.NotificationListResponse;       // 목록 + 안 읽은 개수
import com.example.budget.notification.dto.NotificationResponse;           // 알림 한 건
import lombok.RequiredArgsConstructor;                                     // final 필드 생성자 자동 생성
import org.springframework.stereotype.Service;                             // Service 표시
import org.springframework.transaction.annotation.Transactional;           // 트랜잭션

import java.util.List;   // 여러 개 결과

@Service                        // 스프링이 Service로 등록·관리
@RequiredArgsConstructor        // final 필드를 받는 생성자 자동 생성
@Transactional(readOnly = true) // 기본은 읽기 전용
public class NotificationService {

    private final NotificationRepository notificationRepository;   // 알림 DB 작업

    // ───────────── 알림 조회 (FR-12) ─────────────
    public NotificationListResponse getNotifications(Long userId, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)   // 안 읽은 것만
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);                // 전부

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);   // 🔔 숫자 (목록 조건과 상관없이 항상 안 읽은 개수)

        return new NotificationListResponse(
                unreadCount,
                notifications.stream()
                        .map(NotificationResponse::from)   // 엔티티 → 응답 상자
                        .toList());
    }

    // ───────────── 읽음 처리 (FR-12) ─────────────
    @Transactional   // 값을 바꾸므로 읽기 전용 해제
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)   // 본인 알림인지 확인
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));            // 남의 것이거나 없으면 404

        notification.markAsRead();                        // isRead = true (변경 감지로 자동 UPDATE)
        return NotificationResponse.from(notification);   // 바뀐 알림 돌려줌
    }
}