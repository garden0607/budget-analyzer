package com.example.budget.budget;   // budget 패키지

import com.example.budget.budget.dto.BudgetRequest;            // 예산 설정 요청 상자
import com.example.budget.budget.dto.BudgetResponse;           // 예산 응답 상자
import com.example.budget.category.Category;                   // 카테고리 엔티티
import com.example.budget.category.CategoryRepository;         // 카테고리 저장소
import com.example.budget.category.TransactionType;            // 수입/지출 enum
import com.example.budget.global.exception.BusinessException;  // 우리가 만든 예외
import com.example.budget.global.exception.ErrorCode;          // 오류 목록표
import com.example.budget.user.User;                           // 회원 엔티티
import com.example.budget.user.UserRepository;                 // 회원 저장소
import lombok.RequiredArgsConstructor;                         // final 필드 생성자 자동 생성
import org.springframework.stereotype.Service;                 // Service 표시
import org.springframework.transaction.annotation.Transactional; // 트랜잭션

import java.util.Optional;   // 있을 수도 없을 수도 있는 값

@Service                        // 스프링이 Service로 등록·관리
@RequiredArgsConstructor        // final 필드를 받는 생성자 자동 생성
@Transactional(readOnly = true) // 기본은 읽기 전용
public class BudgetService {

    private final BudgetRepository budgetRepository;       // 예산 DB 작업
    private final CategoryRepository categoryRepository;   // 카테고리 DB 작업
    private final UserRepository userRepository;           // 회원 DB 작업

    // ───────────── 예산 설정 (FR-09) ─────────────
    @Transactional   // 저장하므로 읽기 전용 해제
    public BudgetResponse setBudget(Long userId, BudgetRequest request) {

        // 1. 본인 카테고리인지 확인 (남의 것이면 404)
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. 지출 카테고리인지 확인 (수입 카테고리면 400)
        if (category.getType() != TransactionType.EXPENSE) {
            throw new BusinessException(ErrorCode.BUDGET_EXPENSE_ONLY);
        }

        // 3. 같은 카테고리·같은 월 예산이 이미 있는지 찾기
        Optional<Budget> existing = budgetRepository.findByCategoryIdAndBudgetMonth(
                category.getId(), request.budgetMonth());

        Budget budget;
        if (existing.isPresent()) {                       // 있으면
            budget = existing.get();                      //   그 예산을 꺼내서
            budget.changeAmount(request.amount());        //   금액만 수정 (변경 감지로 자동 UPDATE)
        } else {                                          // 없으면
            User user = userRepository.findById(userId)   //   주인 회원 찾고
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            budget = Budget.builder()                     //   새 예산 만들기
                    .user(user)                           //     주인 회원
                    .category(category)                   //     카테고리
                    .budgetMonth(request.budgetMonth())   //     적용 월 "2026-11"
                    .amount(request.amount())             //     예산 금액
                    .build();                             //     상태는 NORMAL로 시작 (엔티티 생성자에서)
            budgetRepository.save(budget);                //   budgets 테이블에 INSERT
        }

        // TODO(10주차 2일차): 이 (카테고리, 월)의 사용률 재계산 + 알림 판정
        //   예: 이미 9만 원 쓴 상태에서 예산 8만 원 → 바로 EXCEEDED + 초과 알림

        return BudgetResponse.from(budget);   // 설정된 예산과 현재 상태를 돌려줌
    }
}