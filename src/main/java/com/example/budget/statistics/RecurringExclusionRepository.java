package com.example.budget.statistics;   // statistics 패키지

import org.springframework.data.jpa.repository.JpaRepository;   // 기본 저장·조회 기능

import java.util.List;   // 여러 개 결과

// 정기 지출 제외 목록 DB 작업
public interface RecurringExclusionRepository extends JpaRepository<RecurringExclusion, Long> {

    List<RecurringExclusion> findByUserId(Long userId);   // 내 제외 목록 → 결과에서 빼기

    boolean existsByUserIdAndCategoryIdAndName(Long userId, Long categoryId, String name); // 이미 제외했는지 → 중복 저장 방지
}