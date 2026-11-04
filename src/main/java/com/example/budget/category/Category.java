package com.example.budget.category;   // category 패키지

import com.example.budget.global.BaseCreatedEntity;   // 생성일시 자동 기록 부모 클래스
import com.example.budget.user.User;                  // 회원 엔티티 (연관관계)
import jakarta.persistence.*;                         // JPA 어노테이션 전체
import lombok.AccessLevel;                            // 접근 범위 지정
import lombok.Builder;                                // 빌더 패턴 자동 생성
import lombok.Getter;                                 // getter 자동 생성
import lombok.NoArgsConstructor;                      // 기본 생성자 자동 생성

// 카테고리 엔티티 = DB의 categories 테이블
@Getter                                               // getter 자동 생성 (setter 없음)
@NoArgsConstructor(access = AccessLevel.PROTECTED)    // JPA용 빈 생성자, 밖에서 new 금지
@Entity                                               // DB 테이블과 연결
@Table(name = "categories",                           // 테이블 이름 categories
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type", "name"})) // 같은 회원·같은 구분 안에서 이름 중복 불가
public class Category extends BaseCreatedEntity {     // created_at 물려받음

    @Id                                                    // 기본키(PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // DB 자동 증가
    private Long id;                                       // 카테고리 id

    @ManyToOne(fetch = FetchType.LAZY)                     // 회원 1명 : 카테고리 여러 개 (N:1), 회원 정보는 필요할 때만 조회
    @JoinColumn(name = "user_id", nullable = false)        // FK 컬럼 user_id, 빈 값 불가
    private User user;                                     // 이 카테고리의 주인

    @Column(nullable = false, length = 20)                 // 빈 값 불가, 최대 20자
    private String name;                                   // 카테고리 이름 (식비, 급여 ...)

    @Enumerated(EnumType.STRING)                           // enum을 글자("EXPENSE")로 저장 (숫자 0, 1로 저장하지 않음)
    @Column(nullable = false, length = 10)                 // 빈 값 불가, 최대 10자
    private TransactionType type;                          // 구분: INCOME(수입) / EXPENSE(지출)

    @Column(nullable = false)                              // 빈 값 불가 (컬럼명은 자동으로 is_default)
    private boolean isDefault;                             // 기본 카테고리 여부 (가입 시 자동 생성된 것이면 true)

    @Builder                                                                              // Category.builder()...build()로 생성
    private Category(User user, String name, TransactionType type, boolean isDefault) {   // private: 빌더로만 생성
        this.user = user;              // 주인 회원
        this.name = name;              // 이름
        this.type = type;              // 수입/지출 구분
        this.isDefault = isDefault;    // 기본 카테고리 여부
    }

    public void changeName(String name) {   // 이름 수정용 메서드 (setter 대신 의미 있는 이름으로)
        this.name = name;                   // 이름만 바꿀 수 있음, 구분(type)은 못 바꿈
    }
}