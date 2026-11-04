package com.example.budget.budget;   // budget 패키지

import com.example.budget.category.Category;     // 카테고리 엔티티 (연관관계)
import com.example.budget.global.BaseTimeEntity; // 생성일시 + 수정일시 자동 기록 부모 클래스
import com.example.budget.user.User;             // 회원 엔티티 (연관관계)
import jakarta.persistence.*;                    // JPA 어노테이션 전체
import lombok.AccessLevel;                       // 접근 범위 지정
import lombok.Builder;                           // 빌더 패턴 자동 생성
import lombok.Getter;                            // getter 자동 생성
import lombok.NoArgsConstructor;                 // 기본 생성자 자동 생성

// 예산 엔티티 = DB의 budgets 테이블 (카테고리별 월 예산)
@Getter                                               // getter 자동 생성 (setter 없음)
@NoArgsConstructor(access = AccessLevel.PROTECTED)    // JPA용 빈 생성자, 밖에서 new 금지
@Entity                                               // DB 테이블과 연결
@Table(name = "budgets",                              // 테이블 이름 budgets
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "budget_month"})) // 한 카테고리에 같은 달 예산은 1개만
public class Budget extends BaseTimeEntity {          // created_at + updated_at 물려받음 (예산 금액은 수정 가능)

    @Id                                                    // 기본키(PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // DB 자동 증가
    private Long id;                                       // 예산 id

    @ManyToOne(fetch = FetchType.LAZY)                     // 회원 1명 : 예산 여러 개 (N:1)
    @JoinColumn(name = "user_id", nullable = false)        // FK 컬럼 user_id, 빈 값 불가
    private User user;                                     // 예산 주인 (월별 내 예산 목록 조회에 사용)

    @ManyToOne(fetch = FetchType.LAZY)                     // 카테고리 1개 : 예산 여러 개 (달마다 1개씩)
    @JoinColumn(name = "category_id", nullable = false)    // FK 컬럼 category_id, 빈 값 불가
    private Category category;                             // 어느 카테고리의 예산인지

    @Column(nullable = false, length = 7, columnDefinition = "CHAR(7)")  // 빈 값 불가, 항상 7글자 고정 (CHAR)
    private String budgetMonth;                                          // 적용 월 "2026-11" 형식

    @Column(nullable = false)                              // 빈 값 불가
    private int amount;                                    // 예산 금액

    @Enumerated(EnumType.STRING)                           // enum을 글자로 저장
    @Column(nullable = false, length = 10)                 // 빈 값 불가, 최대 10자
    private BudgetStatus status = BudgetStatus.NORMAL;     // 예산 상태: NORMAL / WARNING(80%↑) / EXCEEDED(100%↑), 기본 NORMAL

    @Builder                                                                               // 빌더로 생성
    private Budget(User user, Category category, String budgetMonth, int amount) {         // 상태는 안 받음 → 처음엔 항상 NORMAL
        this.user = user;                   // 주인 회원
        this.category = category;           // 카테고리
        this.budgetMonth = budgetMonth;     // 적용 월
        this.amount = amount;               // 예산 금액
        this.status = BudgetStatus.NORMAL;  // 새 예산은 정상 상태로 시작
    }

    public void changeAmount(int amount) {  // 예산 금액 수정용 메서드
        this.amount = amount;               // 금액 변경 → 이후 사용률 재계산 대상 (10주차)
    }

    public void changeStatus(BudgetStatus status) {   // 예산 상태 변경용 메서드
        this.status = status;                         // 사용률 계산 결과에 따라 NORMAL/WARNING/EXCEEDED로 바꿈
    }
}