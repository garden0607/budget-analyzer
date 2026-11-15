package com.example.budget.budget;   // 테스트할 클래스와 같은 패키지

import com.example.budget.category.Category;                       // ★ 카테고리 엔티티
import com.example.budget.category.TransactionType;                // ★ 수입/지출
import com.example.budget.notification.Notification;               // ★ 알림 엔티티
import com.example.budget.notification.NotificationRepository;     // ★ 알림 저장소 (가짜로)
import com.example.budget.notification.NotificationType;           // ★ 알림 종류
import com.example.budget.transaction.TransactionRepository;       // ★ 거래 저장소 (가짜로)
import com.example.budget.user.User;                               // ★ 회원 엔티티
import org.junit.jupiter.api.DisplayName;                          // 테스트 이름을 한글로
import org.junit.jupiter.api.Test;                                 // 테스트 표시
import org.junit.jupiter.api.extension.ExtendWith;                 // ★ Mockito 켜기
import org.mockito.ArgumentCaptor;                                 // ★ 저장된 값을 붙잡아서 들여다보기
import org.mockito.InjectMocks;                                    // ★ 가짜들을 넣어서 진짜 객체 만들기
import org.mockito.Mock;                                           // ★ 가짜 객체 표시
import org.mockito.junit.jupiter.MockitoExtension;                 // ★ JUnit 5용 Mockito
import org.springframework.test.util.ReflectionTestUtils;          // ★ private 필드에 값 넣기 (id 설정용)
import static org.mockito.ArgumentMatchers.any;   // "아무 값이나"
import static org.mockito.Mockito.never;          // "한 번도 안 불림"
import static org.mockito.Mockito.verifyNoInteractions;   // "이 가짜들은 한 번도 안 불렸어야 함"

import java.time.YearMonth;   // ★ 연·월
import java.util.Optional;    // ★ 있을 수도 없을 수도 있는 값

import static org.assertj.core.api.Assertions.assertThat;   // 결과 검사
import static org.mockito.BDDMockito.given;                 // ★ "이렇게 불리면 이걸 돌려줘"
import static org.mockito.Mockito.verify;                   // ★ "정말 불렸는지 확인"

@ExtendWith(MockitoExtension.class)   // ★ 이 테스트에서 Mockito를 쓴다
class BudgetUsageServiceTest {

    @Mock BudgetRepository budgetRepository;                 // ★ 가짜 예산 저장소
    @Mock TransactionRepository transactionRepository;       // ★ 가짜 거래 저장소
    @Mock NotificationRepository notificationRepository;     // ★ 가짜 알림 저장소
    @InjectMocks BudgetUsageService budgetUsageService;      // ★ 위 가짜 3개를 넣어서 만든 "진짜" BudgetUsageService

    private static final YearMonth NOV = YearMonth.of(2026, 11);   // ★ 테스트용 달: 2026년 11월

    // ───────────── 사용률 계산 (3단계) ─────────────
    @Test
    @DisplayName("사용률 = 지출 × 100 ÷ 예산")
    void usageRateBasic() {
        assertThat(BudgetUsageService.calculateUsageRate(85_000, 100_000)).isEqualTo(85);
        assertThat(BudgetUsageService.calculateUsageRate(9_000, 10_000)).isEqualTo(90);
    }

    @Test
    @DisplayName("예산보다 많이 쓰면 100% 넘게 나옴")
    void usageRateOver100() {
        assertThat(BudgetUsageService.calculateUsageRate(9_000, 8_000)).isEqualTo(112);
    }

    @Test
    @DisplayName("소수점은 버림")
    void usageRateTruncates() {
        assertThat(BudgetUsageService.calculateUsageRate(7_999, 10_000)).isEqualTo(79);
    }

    @Test
    @DisplayName("지출이 0원이면 0%")
    void usageRateZero() {
        assertThat(BudgetUsageService.calculateUsageRate(0, 10_000)).isEqualTo(0);
    }

    // ───────────── 알림 판정 (4단계) ─────────────  ★
    @Test
    @DisplayName("정상 → 경고가 되면 경고 알림이 생긴다")
    void normalToWarningCreatesNotification() {
        // [준비] 교통비 예산 1만 원(상태 NORMAL), 그 달 지출 8천 원인 상황 만들기
        Category category = expenseCategory();
        Budget budget = budget(category, 10_000);                                    // 새 예산은 NORMAL로 시작
        given(budgetRepository.findByCategoryIdAndBudgetMonth(2L, "2026-11"))
                .willReturn(Optional.of(budget));                                    // ② 예산 찾기 → 이 예산을 돌려줘
        given(transactionRepository.sumByCategoryAndPeriod(2L, NOV.atDay(1), NOV.atEndOfMonth()))
                .willReturn(8_000L);                                                 // ③ 합계 → 8,000원을 돌려줘

        // [실행] recalculate 부르기
        budgetUsageService.recalculate(category, NOV);

        // [확인 1] 예산 상태가 WARNING으로 바뀌었나 (⑥)
        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.WARNING);

        // [확인 2] 알림이 저장됐나, 내용이 맞나 (⑤)
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());                      // save가 불렸는지 + 넘어간 알림을 붙잡기
        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.WARNING);            // 경고 알림
        assertThat(saved.getUsageRate()).isEqualTo(80);                             // 사용률 80
        assertThat(saved.getMessage()).isEqualTo("교통비 카테고리 예산의 80%를 사용했습니다");
    }
    @Test
    @DisplayName("이미 경고인데 또 경고면 알림이 생기지 않는다 (중복 방지)")
    void warningToWarningNoNotification() {
        // [준비] 예산 1만 원, 이미 WARNING 상태, 지출 9천 원(90%)
        Category category = expenseCategory();
        Budget budget = budget(category, 10_000);
        budget.changeStatus(BudgetStatus.WARNING);                                   // 지난번에 이미 경고였던 상황
        given(budgetRepository.findByCategoryIdAndBudgetMonth(2L, "2026-11"))
                .willReturn(Optional.of(budget));
        given(transactionRepository.sumByCategoryAndPeriod(2L, NOV.atDay(1), NOV.atEndOfMonth()))
                .willReturn(9_000L);

        // [실행]
        budgetUsageService.recalculate(category, NOV);

        // [확인] 상태는 WARNING 그대로, 알림 저장은 한 번도 안 불림
        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.WARNING);
        verify(notificationRepository, never()).save(any());                         // never(): 한 번도 안 불렸어야 함
    }

    @Test
    @DisplayName("초과 → 경고로 내려가면 알림은 없고 상태만 저장된다")
    void exceededToWarningNoNotificationButStatusSaved() {
        // [준비] 예산 1만 원, EXCEEDED 상태였는데 거래를 지워서 지출 9천 원(90%)
        Category category = expenseCategory();
        Budget budget = budget(category, 10_000);
        budget.changeStatus(BudgetStatus.EXCEEDED);                                  // 지난번에 초과였던 상황
        given(budgetRepository.findByCategoryIdAndBudgetMonth(2L, "2026-11"))
                .willReturn(Optional.of(budget));
        given(transactionRepository.sumByCategoryAndPeriod(2L, NOV.atDay(1), NOV.atEndOfMonth()))
                .willReturn(9_000L);

        // [실행]
        budgetUsageService.recalculate(category, NOV);

        // [확인] 상태는 WARNING으로 내려가 저장됐고, 알림은 없음
        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.WARNING);              // ⑥은 항상 실행
        verify(notificationRepository, never()).save(any());                         // ⑤는 실행 안 됨
    }
    @Test
    @DisplayName("수입 카테고리는 아무것도 계산하지 않는다")
    void incomeCategoryDoesNothing() {
        // [준비] 수입 카테고리(급여)
        User user = User.builder().email("test@test.com").password("x").nickname("테스트").build();
        Category income = Category.builder()
                .user(user).name("급여").type(TransactionType.INCOME).isDefault(true)
                .build();

        // [실행]
        budgetUsageService.recalculate(income, NOV);

        // [확인] ①에서 바로 끝나서 가짜 Repository 셋 다 한 번도 안 불림
        verifyNoInteractions(budgetRepository, transactionRepository, notificationRepository);
    }

    @Test
    @DisplayName("그 달 예산이 없으면 합계도 알림도 없다")
    void noBudgetDoesNothing() {
        // [준비] 지출 카테고리지만 예산을 찾으면 비어 있음
        Category category = expenseCategory();
        given(budgetRepository.findByCategoryIdAndBudgetMonth(2L, "2026-11"))
                .willReturn(Optional.empty());                                       // ② 예산 없음

        // [실행]
        budgetUsageService.recalculate(category, NOV);

        // [확인] ②에서 끝나서 합계 조회(③)도, 알림 저장(⑤)도 안 함
        verifyNoInteractions(transactionRepository, notificationRepository);
    }

    // ───────────── 테스트용 데이터 만드는 도우미 ─────────────  ★
    private Category expenseCategory() {
        User user = User.builder().email("test@test.com").password("x").nickname("테스트").build();
        Category category = Category.builder()
                .user(user).name("교통비").type(TransactionType.EXPENSE).isDefault(true)
                .build();
        ReflectionTestUtils.setField(category, "id", 2L);   // DB에 저장 안 해서 id가 없으니, 테스트용으로 2를 직접 넣음
        return category;
    }

    private Budget budget(Category category, int amount) {
        return Budget.builder()
                .user(category.getUser()).category(category).budgetMonth("2026-11").amount(amount)
                .build();                                   // 상태는 생성자에서 NORMAL
    }
}