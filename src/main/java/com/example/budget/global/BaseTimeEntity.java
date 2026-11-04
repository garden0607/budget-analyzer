package com.example.budget.global;   // global 패키지

import jakarta.persistence.Column;                        // 컬럼 설정
import jakarta.persistence.MappedSuperclass;             // 테이블 없이 필드만 물려주는 부모 클래스 표시
import lombok.Getter;                                    // getter 자동 생성
import org.springframework.data.annotation.LastModifiedDate;   // 수정될 때마다 시간 자동 기록

import java.time.LocalDateTime;   // 날짜 + 시간 타입

// 생성일시 + 수정일시를 자동으로 넣어 주는 공통 부모 클래스
// transactions, budgets가 상속 (내용이 수정될 수 있는 테이블)
@Getter                                                           // getUpdatedAt() 자동 생성
@MappedSuperclass                                                 // 테이블이 안 되고 필드만 자식 테이블에 들어감
public abstract class BaseTimeEntity extends BaseCreatedEntity {  // BaseCreatedEntity를 물려받아 created_at도 같이 가짐

    @LastModifiedDate                                 // INSERT·UPDATE될 때마다 현재 시간으로 자동 갱신
    @Column(name = "updated_at", nullable = false)    // 컬럼명 updated_at, 빈 값 불가 (수정은 가능)
    private LocalDateTime updatedAt;                  // 수정일시
}
// @EntityListeners는 부모(BaseCreatedEntity)에 붙어 있어서 여기엔 다시 안 붙여도 됨