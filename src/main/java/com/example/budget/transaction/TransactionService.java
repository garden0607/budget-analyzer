package com.example.budget.transaction;   // transaction 패키지

import com.example.budget.category.Category;                     // 카테고리 엔티티
import com.example.budget.category.CategoryRepository;           // 카테고리 저장소
import com.example.budget.global.exception.BusinessException;    // 우리가 만든 예외
import com.example.budget.global.exception.ErrorCode;            // 오류 목록표
import com.example.budget.transaction.dto.TransactionRequest;    // 등록·수정 요청 상자
import com.example.budget.transaction.dto.TransactionResponse;   // 응답 상자
import com.example.budget.user.User;                             // 회원 엔티티
import com.example.budget.user.UserRepository;                   // 회원 저장소
import lombok.RequiredArgsConstructor;                           // final 필드 생성자 자동 생성
import org.springframework.stereotype.Service;                   // Service 표시
import org.springframework.transaction.annotation.Transactional; // 트랜잭션

import java.time.LocalDate;   // 날짜 타입
import java.time.ZoneId;      // 시간대

@Service                        // 스프링이 Service로 등록·관리
@RequiredArgsConstructor        // final 필드를 받는 생성자 자동 생성
@Transactional(readOnly = true) // 기본은 읽기 전용
public class TransactionService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");   // 한국 기준 "오늘"

    private final TransactionRepository transactionRepository;   // 거래 DB 작업
    private final CategoryRepository categoryRepository;         // 카테고리 DB 작업
    private final UserRepository userRepository;                 // 회원 DB 작업

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

    // ───────────── 수정 (FR-06) ─────────────  ★ 추가
    @Transactional
    public TransactionResponse update(Long userId, Long transactionId, TransactionRequest request) {
        Transaction transaction = findMyTransaction(userId, transactionId);   // 1. 본인 거래인지 확인 (아니면 404)
        validateDate(request.transactionDate());                              // 2. 거래일 1년 이내 (등록과 같은 검증)
        Category newCategory = findMyCategory(userId, request.categoryId());  // 3. 바꿀 카테고리도 본인 것인지 확인

        Category oldCategory = transaction.getCategory();           // 4. 변경 전 카테고리 기억 (10주차 재계산용)
        LocalDate oldDate = transaction.getTransactionDate();       //    변경 전 날짜 기억 (어느 달인지)

        transaction.update(newCategory,                              // 5. 값 변경 (변경 감지로 자동 UPDATE)
                request.amount(),
                request.transactionDate(),
                normalizeMemo(request.memo()));

        // TODO(10주차): 변경 전 (oldCategory, oldDate의 월)과 변경 후 (newCategory, 새 날짜의 월) 각각 사용률 재계산

        return TransactionResponse.from(transaction);               // 바뀐 거래 돌려줌
    }

    // ───────────── 삭제 (FR-07) ─────────────  ★ 추가
    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction transaction = findMyTransaction(userId, transactionId);   // 1. 본인 거래인지 확인 (아니면 404)
        transactionRepository.delete(transaction);                            // 2. 삭제

        // TODO(10주차): 삭제된 거래의 (카테고리, 월) 사용률 재계산
    }

    // ───────────── 공통: 본인 거래 찾기 ─────────────  ★ 추가
    private Transaction findMyTransaction(Long userId, Long transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)                // id와 주인이 둘 다 맞아야 찾아짐
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND)); // 남의 거래거나 없으면 404
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
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_DATE);   // 400
        }
    }

    // ───────────── 공통: 메모 정리 ─────────────
    private String normalizeMemo(String memo) {
        if (memo == null || memo.isBlank()) {   // 없거나 공백뿐이면
            return null;                        // 메모 없음
        }
        return memo.strip();                    // 앞뒤 공백 제거
    }
}