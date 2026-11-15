package com.example.budget.transaction;   // 테스트할 TransactionService와 같은 패키지

import com.example.budget.budget.BudgetUsageService;              // 재계산 (가짜로)
import com.example.budget.category.Category;                      // 카테고리 엔티티
import com.example.budget.category.CategoryRepository;            // 카테고리 저장소 (가짜로)
import com.example.budget.category.TransactionType;               // 수입/지출
import com.example.budget.transaction.dto.TransactionRequest;     // 수정 요청 상자
import com.example.budget.user.User;                              // 회원 엔티티
import com.example.budget.user.UserRepository;                    // 회원 저장소 (가짜로)
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;   // 날짜
import java.time.YearMonth;   // 연·월
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;      // 아무 값이나
import static org.mockito.BDDMockito.given;          // 이렇게 불리면 이걸 돌려줘
import static org.mockito.Mockito.times;             // 몇 번 불렸는지
import static org.mockito.Mockito.verify;            // 정말 불렸는지 확인

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;   // 가짜 거래 저장소
    @Mock CategoryRepository categoryRepository;         // 가짜 카테고리 저장소
    @Mock UserRepository userRepository;                 // 가짜 회원 저장소 (수정에선 안 쓰지만 생성자에 필요)
    @Mock BudgetUsageService budgetUsageService;         // 가짜 재계산 → "몇 번, 무엇으로 불렸나"만 확인
    @InjectMocks TransactionService transactionService;  // 위 가짜 4개를 넣어 만든 진짜 TransactionService

    private static final Long USER_ID = 1L;          // 로그인한 회원 id
    private static final Long TX_ID = 10L;           // 수정할 거래 id
    private static final YearMonth NOV = YearMonth.of(2026, 11);

    @Test
    @DisplayName("카테고리를 바꾸면 변경 전·후 두 곳을 다시 계산한다")
    void changeCategoryRecalculatesBoth() {
        // [준비] 교통비 8,000원(11/5) 거래가 있고, 이걸 식비로 옮기려 함
        User user = user();
        Category transport = category(user, 2L, "교통비");
        Category food = category(user, 1L, "식비");
        Transaction tx = Transaction.builder()
                .user(user).category(transport).amount(8_000)
                .transactionDate(LocalDate.of(2026, 11, 5)).memo("택시")
                .build();
        given(transactionRepository.findByIdAndUserId(TX_ID, USER_ID)).willReturn(Optional.of(tx));   // 내 거래 찾기 → tx
        given(categoryRepository.findByIdAndUserId(1L, USER_ID)).willReturn(Optional.of(food));       // 새 카테고리 → 식비

        TransactionRequest request = new TransactionRequest(1L, 8_000, LocalDate.of(2026, 11, 5), "택시");   // 식비로 변경

        // [실행]
        transactionService.update(USER_ID, TX_ID, request);

        // [확인] 교통비 11월(변경 전), 식비 11월(변경 후) 둘 다 recalculate가 불렸나
        verify(budgetUsageService).recalculate(transport, NOV);   // 변경 전: 사용률 ↓
        verify(budgetUsageService).recalculate(food, NOV);        // 변경 후: 사용률 ↑
    }

    @Test
    @DisplayName("금액만 바꾸면 한 번만 다시 계산한다")
    void changeAmountOnlyRecalculatesOnce() {
        // [준비] 교통비 8,000원(11/5) → 같은 교통비, 같은 날, 금액만 9,000원
        User user = user();
        Category transport = category(user, 2L, "교통비");
        Transaction tx = Transaction.builder()
                .user(user).category(transport).amount(8_000)
                .transactionDate(LocalDate.of(2026, 11, 5)).memo("택시")
                .build();
        given(transactionRepository.findByIdAndUserId(TX_ID, USER_ID)).willReturn(Optional.of(tx));
        given(categoryRepository.findByIdAndUserId(2L, USER_ID)).willReturn(Optional.of(transport));

        TransactionRequest request = new TransactionRequest(2L, 9_000, LocalDate.of(2026, 11, 5), "택시");

        // [실행]
        transactionService.update(USER_ID, TX_ID, request);

        // [확인] (교통비, 11월)로 딱 한 번만 불렸나
        verify(budgetUsageService, times(1)).recalculate(any(), any());   // 전체 호출 횟수 = 1
        verify(budgetUsageService).recalculate(transport, NOV);
    }

    // ───────────── 테스트용 데이터 도우미 ─────────────
    private User user() {
        return User.builder().email("test@test.com").password("x").nickname("테스트").build();
    }

    private Category category(User user, Long id, String name) {
        Category category = Category.builder()
                .user(user).name(name).type(TransactionType.EXPENSE).isDefault(true)
                .build();
        ReflectionTestUtils.setField(category, "id", id);   // 테스트용 id 직접 넣기
        return category;
    }
}