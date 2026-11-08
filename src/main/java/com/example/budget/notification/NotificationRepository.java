package com.example.budget.notification;   // notification 패키지

import org.springframework.data.jpa.repository.JpaRepository;   // 기본 저장·조회 기능

import java.util.List;       // 여러 개 결과
import java.util.Optional;   // 하나 또는 없음

// 알림 DB 작업
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);   // 내 알림을 최신순으로 → 알림 드롭다운

    long countByUserIdAndIsReadFalse(Long userId);                       // 내 안 읽은 알림 개수 → 🔔 숫자 표시

    Optional<Notification> findByIdAndUserId(Long id, Long userId);      // 내 알림만 찾기 → 읽음 처리 시 남의 알림 차단
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);   // 내 안 읽은 알림만 최신순 → "안 읽은 것만 보기"
}