package com.example.budget.category;   // category 패키지

import org.springframework.data.jpa.repository.JpaRepository;   // 기본 저장·조회 기능

import java.util.List;       // 여러 개 결과
import java.util.Optional;   // 하나 또는 없음

// 카테고리 DB 작업
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdAndType(Long userId, TransactionType type);   // 내 카테고리 중 수입만 / 지출만 → 카테고리 탭 화면

    List<Category> findByUserId(Long userId);                                // 내 카테고리 전체 → 거래 등록 드롭다운

    boolean existsByUserIdAndTypeAndName(Long userId, TransactionType type, String name); // 같은 이름 있는지 → 추가·수정 시 중복 확인

    Optional<Category> findByIdAndUserId(Long id, Long userId);              // id + 주인 둘 다 맞는 것만 → 남의 카테고리 접근 차단
}