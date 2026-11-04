package com.example.budget.notification;   // notification 패키지

import com.example.budget.global.BaseCreatedEntity;   // 생성일시 자동 기록 부모 클래스 (알림은 수정 안 하니까 created_at만)
import com.example.budget.user.User;                  // 회원 엔티티 (연관관계)
import jakarta.persistence.*;                         // JPA 어노테이션 전체
import lombok.AccessLevel;                            // 접근 범위 지정
import lombok.Builder;                                // 빌더 패턴 자동 생성
import lombok.Getter;                                 // getter 자동 생성
import lombok.NoArgsConstructor;                      // 기본 생성자 자동 생성

// 예산 알림 엔티티 = DB의 notifications 테이블
@Getter                                               // getter 자동 생성 (setter 없음)
@NoArgsConstructor(access = AccessLevel.PROTECTED)    // JPA용 빈 생성자, 밖에서 new 금지
@Entity                                               // DB 테이블과 연결
@Table(name = "notifications",                        // 테이블 이름 notifications
        indexes = @Index(name = "idx_noti_user_read", columnList = "user_id, is_read")) // "내 안 읽은 알림 개수" 조회를 빠르게
public class Notification extends BaseCreatedEntity { // created_at 물려받음 (알림 발생 시각)

    @Id                                                    // 기본키(PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // DB 자동 증가
    private Long id;                                       // 알림 id

    @ManyToOne(fetch = FetchType.LAZY)                     // 회원 1명 : 알림 여러 개 (N:1)
    @JoinColumn(name = "user_id", nullable = false)        // FK 컬럼 user_id, 빈 값 불가
    private User user;                                     // 알림 받는 회원

    @Enumerated(EnumType.STRING)                           // enum을 글자로 저장
    @Column(nullable = false, length = 10)                 // 빈 값 불가, 최대 10자
    private NotificationType type;                         // 알림 종류: WARNING(80%↑) / EXCEEDED(100%↑)

    @Column(nullable = false, length = 20)                 // 빈 값 불가, 최대 20자 (카테고리 이름 길이와 같음)
    private String categoryName;                           // 카테고리 이름을 FK 대신 글자로 복사 저장

    @Column(nullable = false, length = 7, columnDefinition = "CHAR(7)")  // 빈 값 불가, 7글자 고정
    private String budgetMonth;                                          // 어느 달 예산 알림인지 ("2026-11")

    @Column(nullable = false)                              // 빈 값 불가
    private int usageRate;                                 // 알림 발생 당시 사용률 (예: 85)

    @Column(nullable = false, length = 200)                // 빈 값 불가, 최대 200자
    private String message;                                // 화면에 보여줄 문구 ("식비 카테고리 예산의 85%를 사용했습니다")

    @Column(nullable = false)                              // 빈 값 불가 (컬럼명은 자동으로 is_read)
    private boolean isRead = false;                        // 읽음 여부, 기본은 안 읽음

    @Builder                                                                    // 빌더로 생성
    private Notification(User user, NotificationType type, String categoryName,
                         String budgetMonth, int usageRate, String message) {   // isRead는 안 받음 → 항상 안 읽음으로 시작
        this.user = user;                   // 받는 회원
        this.type = type;                   // 알림 종류
        this.categoryName = categoryName;   // 카테고리 이름 (복사본)
        this.budgetMonth = budgetMonth;     // 적용 월
        this.usageRate = usageRate;         // 사용률
        this.message = message;             // 표시 문구
        this.isRead = false;                // 새 알림은 안 읽음 상태
    }

    public void markAsRead() {   // 알림 읽음 처리 메서드 (드롭다운에서 클릭했을 때)
        this.isRead = true;      // 읽음으로 변경 (다시 안 읽음으로 되돌리는 메서드는 없음)
    }
}