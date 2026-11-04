package com.example.budget.transaction;   // transaction 패키지

import com.example.budget.category.Category;     // 카테고리 엔티티 (연관관계)
import com.example.budget.global.BaseTimeEntity; // 생성일시 + 수정일시 자동 기록 부모 클래스
import com.example.budget.user.User;             // 회원 엔티티 (연관관계)
import jakarta.persistence.*;                    // JPA 어노테이션 전체
import lombok.AccessLevel;                       // 접근 범위 지정
import lombok.Builder;                           // 빌더 패턴 자동 생성
import lombok.Getter;                            // getter 자동 생성
import lombok.NoArgsConstructor;                 // 기본 생성자 자동 생성

import java.time.LocalDate;   // 날짜만 (시간 없음) 타입

// 거래(수입·지출 내역) 엔티티 = DB의 transactions 테이블
@Getter                                               // getter 자동 생성 (setter 없음)
@NoArgsConstructor(access = AccessLevel.PROTECTED)    // JPA용 빈 생성자, 밖에서 new 금지
@Entity                                               // DB 테이블과 연결
@Table(name = "transactions",                         // 테이블 이름 transactions
        indexes = {                                   // 자주 검색하는 조합에 색인을 만들어 조회를 빠르게
                @Index(name = "idx_tx_user_date", columnList = "user_id, transaction_date"),         // "내 거래를 기간별로" 조회용
                @Index(name = "idx_tx_category_date", columnList = "category_id, transaction_date")  // "카테고리별 월 지출 합계" 계산용
        })
public class Transaction extends BaseTimeEntity {     // created_at + updated_at 물려받음 (거래는 수정 가능하니까)

    @Id                                                    // 기본키(PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // DB 자동 증가
    private Long id;                                       // 거래 id

    @ManyToOne(fetch = FetchType.LAZY)                     // 회원 1명 : 거래 여러 개 (N:1), 필요할 때만 조회
    @JoinColumn(name = "user_id", nullable = false)        // FK 컬럼 user_id, 빈 값 불가
    private User user;                                     // 이 거래의 주인 ("남의 거래 못 보게" 검사에 사용)

    @ManyToOne(fetch = FetchType.LAZY)                     // 카테고리 1개 : 거래 여러 개 (N:1)
    @JoinColumn(name = "category_id", nullable = false)    // FK 컬럼 category_id, 빈 값 불가
    private Category category;                             // 거래 카테고리 (수입/지출 구분도 여기서 정해짐)

    @Column(nullable = false)                              // 빈 값 불가
    private int amount;                                    // 금액 (1원 ~ 1,000만 원은 DTO에서 검사 예정)

    @Column(nullable = false)                              // 빈 값 불가 (컬럼명은 자동으로 transaction_date)
    private LocalDate transactionDate;                     // 거래일 (오늘부터 1년 이내 미래까지 허용 예정)

    @Column(length = 100)                                  // 최대 100자, 빈 값 허용 (nullable 생략 = 허용)
    private String memo;                                   // 메모 (선택 입력)

    @Builder                                                                                                   // 빌더로 생성
    private Transaction(User user, Category category, int amount, LocalDate transactionDate, String memo) {    // private: 빌더로만 생성
        this.user = user;                       // 주인 회원
        this.category = category;               // 카테고리
        this.amount = amount;                   // 금액
        this.transactionDate = transactionDate; // 거래일
        this.memo = memo;                       // 메모
    }

    public void update(Category category, int amount, LocalDate transactionDate, String memo) {   // 거래 수정용 메서드
        this.category = category;               // 카테고리 변경 (수입↔지출도 카테고리 따라 바뀜)
        this.amount = amount;                   // 금액 변경
        this.transactionDate = transactionDate; // 거래일 변경
        this.memo = memo;                       // 메모 변경
    }   // 주인(user)은 바꿀 수 없음 → 메서드에 user가 없음
}