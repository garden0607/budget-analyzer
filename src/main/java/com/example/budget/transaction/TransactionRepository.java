package com.example.budget.transaction;   // transaction 패키지

import org.springframework.data.jpa.repository.JpaRepository;   // 기본 저장·조회 기능

import java.util.Optional;   // 하나 또는 없음

// 거래 DB 작업 (기간 검색·합계 쿼리는 9~10주차에 추가 예정)
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);   // 내 거래만 찾기 → 수정·삭제 시 남의 거래 차단

    boolean existsByCategoryId(Long categoryId);                     // 이 카테고리를 쓰는 거래가 있는지 → 카테고리 삭제 금지 판단
}