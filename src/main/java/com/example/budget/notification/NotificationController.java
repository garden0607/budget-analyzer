package com.example.budget.notification;   // notification 패키지

import com.example.budget.notification.dto.NotificationListResponse;   // 목록 + 안 읽은 개수
import com.example.budget.notification.dto.NotificationResponse;       // 알림 한 건
import lombok.RequiredArgsConstructor;                                 // final 필드 생성자 자동 생성
import org.springframework.http.ResponseEntity;                        // 상태 코드 + 내용 응답
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 토큰의 회원 id 꺼내기
import org.springframework.web.bind.annotation.*;                      // @GetMapping, @PatchMapping 등 전체

@RestController                       // API 창구, 돌려주는 값은 자동으로 JSON
@RequestMapping("/api/notifications") // 이 클래스의 모든 주소 앞에 /api/notifications
@RequiredArgsConstructor              // final 필드 생성자 자동 생성
public class NotificationController {

    private final NotificationService notificationService;   // 실제 처리는 Service에 맡김

    // 알림 조회: GET /api/notifications  또는  GET /api/notifications?unreadOnly=true
    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal Long userId,                           // 토큰에서 꺼낸 회원 id
            @RequestParam(defaultValue = "false") boolean unreadOnly) {     // 안 보내면 false → 전체
        return ResponseEntity.ok(notificationService.getNotifications(userId, unreadOnly));   // 200 + 목록 + 안 읽은 개수
    }

    // 읽음 처리: PATCH /api/notifications/3/read
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal Long userId,                           // 회원 id
            @PathVariable Long notificationId) {                            // 주소의 3
        return ResponseEntity.ok(notificationService.markAsRead(userId, notificationId));   // 200 + 읽음 처리된 알림
    }
}