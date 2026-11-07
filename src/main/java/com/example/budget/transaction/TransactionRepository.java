package com.example.budget.transaction;   // transaction 패키지

import com.example.budget.category.TransactionType;          // 수입/지출 enum (검색 조건)
import org.springframework.data.domain.Page;                 // 페이지 결과
import org.springframework.data.domain.Pageable;             // 페이지 요청
import org.springframework.data.jpa.repository.JpaRepository;   // 기본 저장·조회 기능
import org.springframework.data.jpa.repository.Query;        // JPQL 직접 작성
import org.springframework.data.repository.query.Param;      // 쿼리의 :이름 빈칸에 값 연결

import java.time.LocalDate;   // 날짜 타입
import java.util.Optional;    // 하나 또는 없음

// 거래 DB 작업
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);   // 내 거래만 찾기 → 수정·삭제 시 남의 거래 차단

    boolean existsByCategoryId(Long categoryId);                     // 이 카테고리를 쓰는 거래가 있는지 → 카테고리 삭제 금지 판단

    // 거래 내역 검색 (FR-08)
    @Query(value = """
            SELECT t FROM Transaction t
            JOIN FETCH t.category c
            WHERE t.user.id = :userId
              AND t.transactionDate BETWEEN :startDate AND :endDate
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:type IS NULL OR c.type = :type)
            """,
            countQuery = """
            SELECT COUNT(t) FROM Transaction t
            JOIN t.category c
            WHERE t.user.id = :userId
              AND t.transactionDate BETWEEN :startDate AND :endDate
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:type IS NULL OR c.type = :type)
            """)
    Page<Transaction> search(@Param("userId") Long userId,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             @Param("categoryId") Long categoryId,
                             @Param("type") TransactionType type,
                             Pageable pageable);

    // 같은 조건에서 수입 또는 지출 합계 (FR-08)
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            JOIN t.category c
            WHERE t.user.id = :userId
              AND t.transactionDate BETWEEN :startDate AND :endDate
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND c.type = :sumType
            """)
    Long sumAmount(@Param("userId") Long userId,
                   @Param("startDate") LocalDate startDate,
                   @Param("endDate") LocalDate endDate,
                   @Param("categoryId") Long categoryId,
                   @Param("sumType") TransactionType sumType);

    // ★ 추가: 카테고리 하나의 기간 내 금액 합계 → 예산 사용률 계산용 (FR-10, FR-11)
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.category.id = :categoryId
              AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    Long sumByCategoryAndPeriod(@Param("categoryId") Long categoryId,    // 어느 카테고리 (예: 식비)
                                @Param("startDate") LocalDate startDate, // 그 달 1일 (예: 2026-11-01)
                                @Param("endDate") LocalDate endDate);    // 그 달 말일 (예: 2026-11-30)
}