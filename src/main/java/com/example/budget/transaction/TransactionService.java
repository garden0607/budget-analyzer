package com.example.budget.transaction;   // transaction 패키지

import com.example.budget.category.Category;                       // 카테고리 엔티티
import com.example.budget.category.CategoryRepository;             // 카테고리 저장소
import com.example.budget.category.TransactionType;                // ★ 추가: 수입/지출 enum
import com.example.budget.global.exception.BusinessException;      // 우리가 만든 예외
import com.example.budget.global.exception.ErrorCode;              // 오류 목록표
import com.example.budget.transaction.dto.TransactionPageResponse; // ★ 추가: 조회 결과 상자
import com.example.budget.transaction.dto.TransactionRequest;      // 등록·수정 요청 상자
import com.example.budget.transaction.dto.TransactionResponse;     // 응답 상자
import com.example.budget.user.User;                               // 회원 엔티티
import com.example.budget.user.UserRepository;                     // 회원 저장소
import lombok.RequiredArgsConstructor;                             // final 필드 생성자 자동 생성
import org.springframework.data.domain.Page;                       // ★ 추가: 페이지 결과
import org.springframework.data.domain.PageRequest;                // ★ 추가: 페이지 요청서 만들기
import org.springframework.data.domain.Pageable;                   // ★ 추가: 페이지 요청서
import org.springframework.data.domain.Sort;                       // ★ 추가: 정렬
import org.springframework.stereotype.Service;                     // Service 표시
import org.springframework.transaction.annotation.Transactional;   // 트랜잭션

import java.time.LocalDate;   // 날짜 타입
import java.time.YearMonth;   // ★ 추가: 연·월 (이번 달 1일, 말일 계산)
import java.time.ZoneId;      // 시간대

@Service                        // 스프링이 Service로 등록·관리
@RequiredArgsConstructor        // final 필드를 받는 생성자 자동 생성
@Transactional(readOnly = true) // 기본은 읽기 전용
public class TransactionService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");   // 한국 기준 "오늘"
    private static final int PAGE_SIZE = 20;                       // ★ 추가: 한 페이지에 20건 (설계서)

    private final TransactionRepository transactionRepository;   // 거래 DB 작업
    private final CategoryRepository categoryRepository;         // 카테고리 DB 작업
    private final UserRepository userRepository;                 // 회원 DB 작업

    // ───────────── 조회 (FR-08) ─────────────  ★ 추가
    public TransactionPageResponse search(Long userId, LocalDate startDate, LocalDate endDate,
                                          Long categoryId, TransactionType type, int page) {
        // 1. 기간을 안 보냈으면 이번 달 1일 ~ 말일
        YearMonth thisMonth = YearMonth.now(KOREA);                              // 한국 기준 이번 달 (예: 2026-11)
        LocalDate start = (startDate != null) ? startDate : thisMonth.atDay(1);  // 시작일 없으면 11월 1일
        LocalDate end = (endDate != null) ? endDate : thisMonth.atEndOfMonth();  // 종료일 없으면 11월 30일 (예정 거래도 보이게)

        // 2. 기간이 거꾸로면 거부
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);           // 400 "시작일은 종료일보다 늦을 수 없습니다."
        }

        // 3. 카테고리를 보냈으면 본인 것인지 확인 (남의 카테고리 id면 404)
        if (categoryId != null) {
            findMyCategory(userId, categoryId);
        }

        // 4. 날짜 내림차순, 20건씩 검색
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),                                  // 페이지 번호 (음수가 오면 0으로)
                PAGE_SIZE,                                          // 20건씩
                Sort.by(Sort.Order.desc("transactionDate"),         // 최근 날짜가 위로
                        Sort.Order.desc("id")));                    // 같은 날짜면 나중에 등록한 게 위로
        Page<Transaction> result = transactionRepository.search(userId, start, end, categoryId, type, pageable);

        // 5. 수입·지출 합계 (구분을 골랐으면 고른 쪽만 계산, 나머지는 0)
        long incomeTotal = (type == null || type == TransactionType.INCOME)
                ? transactionRepository.sumAmount(userId, start, end, categoryId, TransactionType.INCOME)
                : 0;
        long expenseTotal = (type == null || type == TransactionType.EXPENSE)
                ? transactionRepository.sumAmount(userId, start, end, categoryId, TransactionType.EXPENSE)
                : 0;

        return TransactionPageResponse.of(start, end, result, incomeTotal, expenseTotal);   // 결과 상자로 묶어서 돌려줌
    }

    // ───────────── 등록 (FR-05) ─────────────
    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        validateDate(request.transactionDate());                             // 1. 거래일 1년 이내
        Category category = findMyCategory(userId, request.categoryId());    // 2. 본인 카테고리 확인

        User user = userRepository.findById(userId)                          // 3. 주인 회원 찾기
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Transaction transaction = Transaction.builder()                      // 4. 거래 만들어 저장
                .user(user)
                .category(category)
                .amount(request.amount())
                .transactionDate(request.transactionDate())
                .memo(normalizeMemo(request.memo()))
                .build();
        transactionRepository.save(transaction);

        // TODO(10주차): 지출 거래이면 (카테고리, 월) 사용률 재계산 + 알림 판정

        return TransactionResponse.from(transaction);
    }

    // ───────────── 수정 (FR-06) ─────────────
    @Transactional
    public TransactionResponse update(Long userId, Long transactionId, TransactionRequest request) {
        Transaction transaction = findMyTransaction(userId, transactionId);   // 1. 본인 거래 확인
        validateDate(request.transactionDate());                              // 2. 거래일 1년 이내
        Category newCategory = findMyCategory(userId, request.categoryId());  // 3. 바꿀 카테고리도 본인 것인지

        Category oldCategory = transaction.getCategory();           // 4. 변경 전 카테고리 기억 (10주차용)
        LocalDate oldDate = transaction.getTransactionDate();       //    변경 전 날짜 기억

        transaction.update(newCategory,                              // 5. 값 변경 (변경 감지로 자동 UPDATE)
                request.amount(),
                request.transactionDate(),
                normalizeMemo(request.memo()));

        // TODO(10주차): 변경 전 (oldCategory, oldDate의 월)과 변경 후 (newCategory, 새 날짜의 월) 각각 사용률 재계산

        return TransactionResponse.from(transaction);
    }

    // ───────────── 삭제 (FR-07) ─────────────
    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction transaction = findMyTransaction(userId, transactionId);   // 1. 본인 거래 확인
        transactionRepository.delete(transaction);                            // 2. 삭제

        // TODO(10주차): 삭제된 거래의 (카테고리, 월) 사용률 재계산
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
        LocalDate limit = LocalDate.now(KOREA).plusYears(1);   // 한국 기준 오늘 + 1년
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