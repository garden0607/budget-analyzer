package com.example.budget.statistics;   // statistics 패키지

import com.example.budget.global.BaseCreatedEntity;   // created_at 자동 기록 (8주차에 만든 공통 클래스)
import com.example.budget.user.User;                  // 회원 엔티티
import jakarta.persistence.*;                         // @Entity, @Id, @Column 등
import lombok.AccessLevel;                            // 생성자 접근 범위
import lombok.Builder;                                // 빌더로 생성
import lombok.Getter;                                 // getter 자동 생성
import lombok.NoArgsConstructor;                      // 기본 생성자

// 정기 지출 제외 목록: 사용자가 "아니에요"를 누른 묶음 (카테고리 id + 이름)
@Entity
@Table(name = "recurring_exclusions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category_id", "name"})) // 같은 묶음 중복 제외 방지
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA용 기본 생성자 (밖에서 빈 객체 못 만들게)
public class RecurringExclusion extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // 번호 자동 증가
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)                    // 회원 연결 (필요할 때만 조회)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "category_id", nullable = false)       // 카테고리는 연결 없이 id만 저장 (카테고리 삭제를 막지 않으려고)
    private Long categoryId;

    @Column(nullable = false, length = 100)               // 이름(메모) — 메모 최대 길이 100자와 맞춤
    private String name;

    @Builder
    private RecurringExclusion(User user, Long categoryId, String name) {
        this.user = user;
        this.categoryId = categoryId;
        this.name = name;
    }
}