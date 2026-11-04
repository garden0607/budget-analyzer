package com.example.budget.global;   // global 패키지 (여러 도메인이 같이 쓰는 공통 코드)

import jakarta.persistence.Column;                                  // 컬럼 설정
import jakarta.persistence.EntityListeners;                        // 엔티티 저장·수정 순간을 감시하는 리스너 등록
import jakarta.persistence.MappedSuperclass;                       // 테이블 없이 필드만 물려주는 부모 클래스 표시
import lombok.Getter;                                              // getter 자동 생성
import org.springframework.data.annotation.CreatedDate;            // 처음 저장될 때 시간 자동 기록
import org.springframework.data.jpa.domain.support.AuditingEntityListener; // 시간 자동 기록을 실제로 해 주는 리스너

import java.time.LocalDateTime;   // 날짜 + 시간 타입

// 생성일시(created_at)를 자동으로 넣어 주는 공통 부모 클래스
// users, categories, notifications가 상속 (수정일시가 필요 없는 테이블)
@Getter                                          // getCreatedAt() 자동 생성
@MappedSuperclass                                // 이 클래스 자체는 테이블이 안 됨, 필드만 자식 테이블에 들어감
@EntityListeners(AuditingEntityListener.class)   // 저장 순간을 감시해서 @CreatedDate 칸을 채워 줌
public abstract class BaseCreatedEntity {        // abstract: 직접 객체로 만들지 않고 상속용으로만 사용

    @CreatedDate                                                        // INSERT될 때 현재 시간 자동 입력
    @Column(name = "created_at", nullable = false, updatable = false)   // 컬럼명 created_at, 빈 값 불가, 한 번 저장되면 수정 불가
    private LocalDateTime createdAt;                                    // 생성일시
}
// 동작하려면 BudgetApplication의 @EnableJpaAuditing이 필요함