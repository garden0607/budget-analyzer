package com.example.budget.transaction;   // transaction 패키지

import com.example.budget.budget.BudgetUsageService;               // ★ 추가: 사용률 재계산 + 알림 판정
import com.example.budget.category.Category;                       // 카테고리 엔티티
import com.example.budget.category.CategoryRepository;             // 카테고리 저장소
import com.example.budget.category.TransactionType;                // 수입/지출 enum
import com.example.budget.global.exception.BusinessException;      // 우리가 만든 예외
import com.example.budget.global.exception.ErrorCode;              // 오류 목록표
import com.example.budget.transaction.dto.TransactionPageResponse; // 조회 결과 상자
import com.example.budget.transaction.dto.TransactionRequest;      // 등록·수정 요청 상자
import com.example.budget.transaction.dto.TransactionResponse;     // 응답 상자
import com.example.budget.user.User;                               // 회원 엔티티
import com.example.budget.user.UserRepository;                     // 회원 저장소
import lombok.RequiredArgsConstructor;                             // final 필드 생성자 자동 생성
import org.springframework.data.domain.Page;                       // 페이지 결과
import org.springframework.data.domain.PageRequest;                // 페이지 요청서 만들기
import org.springframework.data.domain.Pageable;                   // 페이지 요청서
import org.springframework.data.domain.Sort;                       // 정렬
import org.springframework.stereotype.Service;                     // Service 표시
import org.springframework.transaction.annotation.Transactional;   // 트랜잭션

import java.time.LocalDate;   // 날짜 타입
import java.time.YearMonth;   // 연·월
import java.time.ZoneId;      // 시간대

@Service                        // 스프링이 Service로 등록·관리
@RequiredArgsConstructor        // final 필드를 받는 생성자 자동 생성
@Transactional(readOnly = true) // 기본은 읽기 전용
public class TransactionService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");   // 한국 기준 "오늘"
    private static final int PAGE_SIZE = 20;                       // 한 페이지에 20건

    private final TransactionRepository transactionRepository;   // 거래 DB 작업
    private final CategoryRepository categoryRepository;         // 카테고리 DB 작업
    private final UserRepository userRepository;                 // 회원 DB 작업
    private final BudgetUsageService budgetUsageService;         // ★ 추가: 사용률 재계산

    // ───────────── 조회 (FR-08) ─────────────
    public TransactionPageResponse search(Long userId, LocalDate startDate, LocalDate endDate,
                                          Long categoryId, TransactionType type, int page) {
        YearMonth thisMonth = YearMonth.now(KOREA);
        LocalDate start = (startDate != null) ? startDate : thisMonth.atDay(1);
        LocalDate end = (endDate != null) ? endDate : thisMonth.atEndOfMonth();

        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (categoryId != null) {
            findMyCategory(userId, categoryId);
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("id")));
        Page<Transaction> result = transactionRepository.search(userId, start, end, categoryId, type, pageable);

        long incomeTotal = (type == null || type == TransactionType.INCOME)
                ? transactionRepository.sumAmount(userId, start, end, categoryId, TransactionType.INCOME)
                : 0;
        long expenseTotal = (type == null || type == TransactionType.EXPENSE)
                ? transactionRepository.sumAmount(userId, start, end, categoryId, TransactionType.EXPENSE)
                : 0;

        return TransactionPageResponse.of(start, end, result, incomeTotal, expenseTotal);
    }

    // ───────────── 등록 (FR-05) ─────────────
    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        validateDate(request.transactionDate());
        Category category = findMyCategory(userId, request.categoryId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Transaction transaction = Transaction.builder()
                .user(user)
                .category(category)
                .amount(request.amount())
                .transactionDate(request.transactionDate())
                .memo(normalizeMemo(request.memo()))
                .build();
        transactionRepository.save(transaction);

        // ★ 추가: 새 거래의 (카테고리, 월) 재계산 (수입이면 recalculate 안에서 바로 끝남)
        budgetUsageService.recalculate(category, YearMonth.from(request.transactionDate()));

        return TransactionResponse.from(transaction);
    }

    // ───────────── 수정 (FR-06) ─────────────
    @Transactional
    public TransactionResponse update(Long userId, Long transactionId, TransactionRequest request) {
        Transaction transaction = findMyTransaction(userId, transactionId);
        validateDate(request.transactionDate());
        Category newCategory = findMyCategory(userId, request.categoryId());

        Category oldCategory = transaction.getCategory();                      // 변경 전 카테고리
        YearMonth oldMonth = YearMonth.from(transaction.getTransactionDate()); // ★ 변경 전 월 (2026-11-05 → 2026-11)

        transaction.update(newCategory,
                request.amount(),
                request.transactionDate(),
                normalizeMemo(request.memo()));

        // ★ 추가: 변경 전 (카테고리, 월) 재계산 → 금액이 빠져서 사용률이 내려갈 수 있음
        budgetUsageService.recalculate(oldCategory, oldMonth);

        // ★ 추가: 변경 후 (카테고리, 월)이 다르면 그쪽도 재계산 → 금액이 더해져서 올라갈 수 있음
        YearMonth newMonth = YearMonth.from(request.transactionDate());
        boolean sameTarget = oldCategory.getId().equals(newCategory.getId()) && oldMonth.equals(newMonth);
        if (!sameTarget) {
            budgetUsageService.recalculate(newCategory, newMonth);
        }

        return TransactionResponse.from(transaction);
    }

    // ───────────── 삭제 (FR-07) ─────────────
    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction transaction = findMyTransaction(userId, transactionId);
        Category category = transaction.getCategory();                      // ★ 지우기 전에 카테고리 기억
        YearMonth month = YearMonth.from(transaction.getTransactionDate()); // ★ 지우기 전에 월 기억

        transactionRepository.delete(transaction);

        // ★ 추가: 지운 거래의 (카테고리, 월) 재계산 → 사용률이 내려감
        budgetUsageService.recalculate(category, month);
    }

    // ───────────── 공통: 본인 거래 찾기 ─────────────
    private Transaction findMyTransaction(Long userId, Long transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    // ───────────── 공통: 본인 카테고리 찾기 ─────────────
    private Category findMyCategory(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    // ───────────── 공통: 거래일 검사 ─────────────
    private void validateDate(LocalDate date) {
        LocalDate limit = LocalDate.now(KOREA).plusYears(1);
        if (date.isAfter(limit)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_DATE);
        }
    }

    // ───────────── 공통: 메모 정리 ─────────────
    private String normalizeMemo(String memo) {
        if (memo == null || memo.isBlank()) {
            return null;
        }
        return memo.strip();
    }
}