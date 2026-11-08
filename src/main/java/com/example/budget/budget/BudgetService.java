package com.example.budget.budget;   // budget 패키지

import com.example.budget.budget.dto.BudgetRequest;            // 예산 설정 요청 상자
import com.example.budget.budget.dto.BudgetResponse;           // 예산 응답 상자
import com.example.budget.budget.dto.BudgetStatusItem;         // ★ 추가: 사용 현황 한 줄
import com.example.budget.budget.dto.BudgetStatusResponse;     // ★ 추가: 사용 현황 전체
import com.example.budget.category.Category;                   // 카테고리 엔티티
import com.example.budget.category.CategoryRepository;         // 카테고리 저장소
import com.example.budget.category.TransactionType;            // 수입/지출 enum
import com.example.budget.global.exception.BusinessException;  // 우리가 만든 예외
import com.example.budget.global.exception.ErrorCode;          // 오류 목록표
import com.example.budget.transaction.TransactionRepository;    // ★ 추가: 카테고리별 지출 합계
import com.example.budget.transaction.dto.CategorySpending;    // ★ 추가: 합계 한 줄 상자
import com.example.budget.user.User;                           // 회원 엔티티
import com.example.budget.user.UserRepository;                 // 회원 저장소
import lombok.RequiredArgsConstructor;                         // final 필드 생성자 자동 생성
import org.springframework.stereotype.Service;                 // Service 표시
import org.springframework.transaction.annotation.Transactional; // 트랜잭션

import java.time.YearMonth;          // 연·월
import java.time.ZoneId;             // ★ 추가: 한국 시간 기준 "이번 달"
import java.util.ArrayList;          // ★ 추가: 목록 만들기
import java.util.Comparator;         // ★ 추가: 정렬 기준
import java.util.List;               // ★ 추가: 여러 개 결과
import java.util.Map;                // ★ 추가: "카테고리 id → 값" 찾아보기표
import java.util.Optional;           // 있을 수도 없을 수도 있는 값
import java.util.stream.Collectors;  // ★ 추가: 목록 → Map 바꾸기

@Service                        // 스프링이 Service로 등록·관리
@RequiredArgsConstructor        // final 필드를 받는 생성자 자동 생성
@Transactional(readOnly = true) // 기본은 읽기 전용
public class BudgetService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");   // ★ 추가: 한국 기준 "이번 달"

    private final BudgetRepository budgetRepository;             // 예산 DB 작업
    private final CategoryRepository categoryRepository;         // 카테고리 DB 작업
    private final UserRepository userRepository;                 // 회원 DB 작업
    private final TransactionRepository transactionRepository;   // ★ 추가: 지출 합계
    private final BudgetUsageService budgetUsageService;         // 사용률 재계산 + 알림 판정

    // ───────────── 예산 사용 현황 조회 (FR-10) ─────────────  ★ 추가
    public BudgetStatusResponse getStatus(Long userId, YearMonth month) {
        YearMonth target = (month != null) ? month : YearMonth.now(KOREA);   // 월을 안 보내면 이번 달

        // 1. 내 지출 카테고리 전부 (예산이 없어도 목록에는 보여줘야 해서) → id 순서로 정렬
        List<Category> categories = categoryRepository.findByUserIdAndType(userId, TransactionType.EXPENSE)
                .stream()
                .sorted(Comparator.comparing(Category::getId))
                .toList();

        // 2. 그 달 예산들 → "카테고리 id → 예산" 찾아보기표
        Map<Long, Budget> budgetByCategory = budgetRepository.findByUserIdAndBudgetMonth(userId, target.toString())
                .stream()
                .collect(Collectors.toMap(b -> b.getCategory().getId(), b -> b));

        // 3. 그 달 카테고리별 지출 합계 (GROUP BY 한 번) → "카테고리 id → 합계" 찾아보기표
        Map<Long, Long> spentByCategory = transactionRepository
                .sumExpenseByCategory(userId, target.atDay(1), target.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(CategorySpending::categoryId, CategorySpending::total));

        // 4. 카테고리마다 한 줄씩 만들기
        List<BudgetStatusItem> items = new ArrayList<>();
        long totalBudget = 0;   // 예산 합계 (예산 있는 카테고리만)
        long totalSpent = 0;    // 사용 합계 (예산 있는 카테고리만)

        for (Category category : categories) {
            long spent = spentByCategory.getOrDefault(category.getId(), 0L);   // 거래가 없으면 0원
            Budget budget = budgetByCategory.get(category.getId());          // 예산이 없으면 null

            if (budget == null) {   // 예산 없음 → 지출만 표시 (설계서 규칙)
                items.add(new BudgetStatusItem(category.getId(), category.getName(),
                        null, spent, null, null, null));
                continue;           // 다음 카테고리로
            }

            int usageRate = BudgetUsageService.calculateUsageRate(spent, budget.getAmount());   // 어제 만든 계산 재사용
            items.add(new BudgetStatusItem(category.getId(), category.getName(),
                    budget.getAmount(),                 // 예산
                    spent,                              // 사용
                    budget.getAmount() - spent,         // 잔여 (초과면 음수)
                    usageRate,                          // 사용률
                    BudgetStatus.of(usageRate)));       // 상태 (매번 계산)

            totalBudget += budget.getAmount();
            totalSpent += spent;
        }

        // 5. 전체 사용률 (예산이 하나도 없으면 null)
        Integer totalUsageRate = (totalBudget > 0)
                ? BudgetUsageService.calculateUsageRate(totalSpent, (int) totalBudget)   // 예산 합계는 최대 수천만 원이라 int 범위 안
                : null;

        return new BudgetStatusResponse(target.toString(), items,
                totalBudget, totalSpent, totalBudget - totalSpent, totalUsageRate);
    }

    // ───────────── 예산 설정 (FR-09) ─────────────
    @Transactional
    public BudgetResponse setBudget(Long userId, BudgetRequest request) {

        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getType() != TransactionType.EXPENSE) {
            throw new BusinessException(ErrorCode.BUDGET_EXPENSE_ONLY);
        }

        Optional<Budget> existing = budgetRepository.findByCategoryIdAndBudgetMonth(
                category.getId(), request.budgetMonth());

        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.changeAmount(request.amount());
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            budget = Budget.builder()
                    .user(user)
                    .category(category)
                    .budgetMonth(request.budgetMonth())
                    .amount(request.amount())
                    .build();
            budgetRepository.save(budget);
        }

        budgetUsageService.recalculate(category, YearMonth.parse(request.budgetMonth()));

        return BudgetResponse.from(budget);
    }
}