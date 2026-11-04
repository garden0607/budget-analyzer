package com.example.budget.budget;   // budget 패키지

import org.springframework.data.jpa.repository.JpaRepository;   // 기본 저장·조회 기능

import java.util.List;       // 여러 개 결과
import java.util.Optional;   // 하나 또는 없음

// 예산 DB 작업
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByCategoryIdAndBudgetMonth(Long categoryId, String budgetMonth); // 특정 카테고리의 그 달 예산 → 설정·재계산

    List<Budget> findByUserIdAndBudgetMonth(Long userId, String budgetMonth);             // 내 그 달 예산 전체 → 예산 화면

    void deleteByCategoryId(Long categoryId);                                             // 카테고리 삭제 시 그 예산도 같이 삭제
}