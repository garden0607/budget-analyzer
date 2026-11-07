package com.example.budget.transaction;   // transaction 패키지

import com.example.budget.category.TransactionType;          // ★ 추가: 수입/지출 enum (검색 조건)
import org.springframework.data.domain.Page;                 // ★ 추가: 페이지 결과 (목록 + 전체 건수 등)
import org.springframework.data.domain.Pageable;             // ★ 추가: 페이지 요청 (몇 페이지, 몇 개, 정렬)
import org.springframework.data.jpa.repository.JpaRepository;   // 기본 저장·조회 기능
import org.springframework.data.jpa.repository.Query;        // ★ 추가: JPQL 직접 작성
import org.springframework.data.repository.query.Param;      // ★ 추가: 쿼리의 :이름 빈칸에 값 연결

import java.time.LocalDate;   // ★ 추가: 날짜 타입 (기간 조건)
import java.util.Optional;    // 하나 또는 없음

// 거래 DB 작업
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);   // 내 거래만 찾기 → 수정·삭제 시 남의 거래 차단

    boolean existsByCategoryId(Long categoryId);                     // 이 카테고리를 쓰는 거래가 있는지 → 카테고리 삭제 금지 판단

    // ★ 추가: 거래 내역 검색 (FR-08) - 조건에 맞는 내 거래를 페이지 단위로
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
    Page<Transaction> search(@Param("userId") Long userId,               // 내 거래만
                             @Param("startDate") LocalDate startDate,    // 시작일
                             @Param("endDate") LocalDate endDate,        // 종료일
                             @Param("categoryId") Long categoryId,       // 카테고리 (null이면 조건 없음)
                             @Param("type") TransactionType type,        // 수입/지출 (null이면 조건 없음)
                             Pageable pageable);                         // 몇 페이지, 20개씩, 날짜 내림차순

    // ★ 추가: 같은 조건에서 수입 또는 지출 합계 (페이지와 상관없이 전체)
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            JOIN t.category c
            WHERE t.user.id = :userId
              AND t.transactionDate BETWEEN :startDate AND :endDate
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND c.type = :sumType
            """)
    Long sumAmount(@Param("userId") Long userId,               // 내 거래만
                   @Param("startDate") LocalDate startDate,    // 시작일
                   @Param("endDate") LocalDate endDate,        // 종료일
                   @Param("categoryId") Long categoryId,       // 카테고리 (null이면 조건 없음)
                   @Param("sumType") TransactionType sumType); // 합계 낼 구분 (INCOME 또는 EXPENSE)
}