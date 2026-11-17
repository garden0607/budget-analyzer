package com.example.budget.statistics;   // statistics 패키지

import com.example.budget.category.Category;                      // 카테고리 엔티티
import com.example.budget.category.CategoryRepository;            // 카테고리 저장소
import com.example.budget.category.TransactionType;               // 수입/지출 enum
import com.example.budget.statistics.dto.CategoryRatioResponse;   // 카테고리 비율 응답
import com.example.budget.statistics.dto.ComparisonResponse;      // ★ 추가: 전월 대비 응답
import com.example.budget.statistics.dto.SummaryResponse;         // 월 합계 응답
import com.example.budget.transaction.TransactionRepository;      // 거래 합계 쿼리
import com.example.budget.transaction.dto.CategorySpending;       // 카테고리별 합계 한 줄
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;            // ★ 추가: 중복 없는 모음 (두 달의 카테고리 합치기)
import java.util.List;
import java.util.Map;
import java.util.Set;                // ★ 추가
import java.util.stream.Collectors;

// 소비 통계 계산 (저장하지 않고 조회할 때마다 거래로 계산)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    // ───────────── 월별 수입·지출 합계 (FR-13) ─────────────
    public SummaryResponse getSummary(Long userId, YearMonth month) {
        YearMonth target = resolveMonth(month);

        long income = transactionRepository.sumAmount(
                userId, target.atDay(1), target.atEndOfMonth(), null, TransactionType.INCOME);
        long expense = transactionRepository.sumAmount(
                userId, target.atDay(1), target.atEndOfMonth(), null, TransactionType.EXPENSE);

        return new SummaryResponse(target.toString(), income, expense, income - expense);
    }

    // ───────────── 카테고리별 지출 비율 (FR-14) ─────────────
    public CategoryRatioResponse getCategoryRatio(Long userId, YearMonth month) {
        YearMonth target = resolveMonth(month);

        List<CategorySpending> spendings = transactionRepository
                .sumExpenseByCategory(userId, target.atDay(1), target.atEndOfMonth());
        long total = spendings.stream().mapToLong(CategorySpending::total).sum();
        Map<Long, String> names = categoryNames(userId);

        List<CategoryRatioResponse.Item> items = spendings.stream()
                .map(s -> new CategoryRatioResponse.Item(
                        s.categoryId(), names.get(s.categoryId()), s.total(), percent(s.total(), total)))
                .sorted(Comparator.comparingLong(CategoryRatioResponse.Item::amount).reversed())
                .toList();

        return new CategoryRatioResponse(target.toString(), total, items);
    }

    // ───────────── 전월 대비 증감 (FR-15) ─────────────  ★ 추가
    public ComparisonResponse getComparison(Long userId, YearMonth month) {
        YearMonth target = resolveMonth(month);         // 이번 달 (예: 2026-11)
        YearMonth previous = target.minusMonths(1);     // 지난달 (예: 2026-10, 1월이면 작년 12월)

        // 1. 두 달 각각 카테고리별 지출 → 찾아보기표 (id → 금액)
        Map<Long, Long> thisMap = spendingMap(userId, target);
        Map<Long, Long> lastMap = spendingMap(userId, previous);

        // 2. 두 달에 나온 카테고리를 빠짐없이 합치기 (한쪽에만 있어도 포함)
        Set<Long> categoryIds = new HashSet<>(thisMap.keySet());   // 이번 달 카테고리들
        categoryIds.addAll(lastMap.keySet());                      // + 지난달 카테고리들 (겹치면 한 번만)

        Map<Long, String> names = categoryNames(userId);

        // 3. 카테고리마다 증감 계산 → 이번 달 금액 큰 순
        List<ComparisonResponse.Item> items = categoryIds.stream()
                .map(id -> {
                    long now = thisMap.getOrDefault(id, 0L);       // 이번 달 (없으면 0원)
                    long before = lastMap.getOrDefault(id, 0L);    // 지난달 (없으면 0원)
                    long diff = now - before;                      // 증감액
                    Double rate = (before == 0) ? null : percent(diff, before);   // 지난달 0원이면 null → "신규"
                    return new ComparisonResponse.Item(id, names.get(id), now, before, diff, rate);
                })
                .sorted(Comparator.comparingLong(ComparisonResponse.Item::thisMonth).reversed())
                .toList();

        return new ComparisonResponse(target.toString(), previous.toString(), items);
    }

    // ───────────── 공통: 조회할 달 정하기 ─────────────
    private YearMonth resolveMonth(YearMonth month) {
        return (month != null) ? month : YearMonth.now(KOREA);
    }

    // ───────────── 공통: 그 달 카테고리별 지출 표 (id → 금액) ─────────────  ★ 추가
    private Map<Long, Long> spendingMap(Long userId, YearMonth month) {
        return transactionRepository.sumExpenseByCategory(userId, month.atDay(1), month.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(CategorySpending::categoryId, CategorySpending::total));
    }

    // ───────────── 공통: 내 카테고리 id → 이름 표 ─────────────
    private Map<Long, String> categoryNames(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    // ───────────── 공통: 비율(%) 계산, 소수점 첫째 자리 반올림 ─────────────
    private double percent(long part, long whole) {
        if (whole == 0) {
            return 0.0;
        }
        return Math.round(part * 1000.0 / whole) / 10.0;
    }
}