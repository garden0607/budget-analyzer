package com.example.budget.statistics;   // 테스트도 같은 패키지 (statistics)

import com.example.budget.statistics.dto.ExpenseRecord;      // 입력: 지출 거래 한 건
import com.example.budget.statistics.dto.RecurringResponse;  // 출력: 정기 지출 한 줄(Item)
import org.junit.jupiter.api.DisplayName;                    // 테스트 이름을 한글로
import org.junit.jupiter.api.Test;                           // 테스트 메서드 표시

import java.time.LocalDate;   // 날짜
import java.util.List;        // 목록

import static org.assertj.core.api.Assertions.assertThat;    // 결과 확인 (AssertJ)

// 정기 지출 자동 탐지 알고리즘 단위 테스트 (설계서 6장 검증 계획)
class RecurringDetectorTest {

    private final RecurringDetector detector = new RecurringDetector(); // DB가 없어서 그냥 new로 생성
    private static final LocalDate TODAY = LocalDate.of(2026, 12, 3);    // "오늘"을 발표일로 고정 (실행 날짜와 상관없이 같은 결과)

    private static final long SUB = 1L;    // 구독 카테고리 id
    private static final long FOOD = 2L;   // 식비 카테고리 id

    // 거래 한 건을 짧게 만드는 도우미: sub(13500, "2026-09-15", "넷플릭스")
    private ExpenseRecord sub(int amount, String date, String memo) {
        return new ExpenseRecord(SUB, "구독", amount, LocalDate.parse(date), memo);
    }

    private ExpenseRecord food(int amount, String date) {
        return new ExpenseRecord(FOOD, "식비", amount, LocalDate.parse(date), "점심");
    }

    // ───────────── 탐지되어야 하는 경우 ─────────────

    @Test
    @DisplayName("1. 매달 같은 날, 같은 금액이 3번이면 정기 지출")
    void detect_sameAmountThreeTimes() {
        // Given: 매달 15일 13,500원 넷플릭스 3번
        List<ExpenseRecord> records = List.of(
                sub(13500, "2026-09-15", "넷플릭스"),
                sub(13500, "2026-10-15", "넷플릭스"),
                sub(13500, "2026-11-15", "넷플릭스"));

        // When
        List<RecurringResponse.Item> result = detector.detect(records, TODAY);

        // Then
        assertThat(result).hasSize(1);                             // 정기 지출 1개
        RecurringResponse.Item item = result.get(0);
        assertThat(item.name()).isEqualTo("넷플릭스");              // 이름은 메모
        assertThat(item.amount()).isEqualTo(13500);                // 최근 금액
        assertThat(item.months()).isEqualTo(3);                    // 3개월 연속
        assertThat(item.dayOfMonth()).isEqualTo(15);               // 매달 15일쯤
        assertThat(item.previousAmount()).isNull();                // 인상 없음
    }

    @Test
    @DisplayName("2. 결제일이 밀려 간격이 33일·26일이어도 정기 지출 (메모가 없으면 카테고리 이름)")
    void detect_shiftedPaymentDate() {
        // Given: 9/15 → 10/18 (33일, 늦게 결제) → 11/13 (26일, 다시 앞당겨짐), 메모 없음
        List<ExpenseRecord> records = List.of(
                sub(9900, "2026-09-15", null),
                sub(9900, "2026-10-18", null),
                sub(9900, "2026-11-13", null));

        List<RecurringResponse.Item> result = detector.detect(records, TODAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("구독");        // 메모가 없으면 카테고리 이름
    }

    @Test
    @DisplayName("3. 중간에 26% 인상돼도 같은 정기 지출로 보고 인상 내용을 알려 준다")
    void detect_priceIncrease() {
        // Given: 13,500원 2번 → 17,000원(26% 인상) 2번
        List<ExpenseRecord> records = List.of(
                sub(13500, "2026-08-15", "넷플릭스"),
                sub(13500, "2026-09-15", "넷플릭스"),
                sub(17000, "2026-10-15", "넷플릭스"),
                sub(17000, "2026-11-15", "넷플릭스"));

        List<RecurringResponse.Item> result = detector.detect(records, TODAY);

        assertThat(result).hasSize(1);
        RecurringResponse.Item item = result.get(0);
        assertThat(item.months()).isEqualTo(4);                    // 갈라지지 않고 4개월 연속
        assertThat(item.amount()).isEqualTo(17000);                // 최근 금액은 오른 금액
        assertThat(item.previousAmount()).isEqualTo(13500);        // 인상 전 금액
        assertThat(item.changedMonth()).isEqualTo("2026-10");      // 10월에 인상
    }

    @Test
    @DisplayName("4. 같은 카테고리에 금액이 비슷한 구독 2개는 섞이지 않고 각각 탐지")
    void detect_twoSimilarSubscriptions() {
        // Given: 스포티파이(매달 5일, 10,900원)와 넷플릭스(매달 15일, 13,500원) → 금액 차이 약 24%로 30% 이내
        List<ExpenseRecord> records = List.of(
                sub(10900, "2026-09-05", "스포티파이"),
                sub(13500, "2026-09-15", "넷플릭스"),
                sub(10900, "2026-10-05", "스포티파이"),
                sub(13500, "2026-10-15", "넷플릭스"),
                sub(10900, "2026-11-05", "스포티파이"),
                sub(13500, "2026-11-15", "넷플릭스"));

        List<RecurringResponse.Item> result = detector.detect(records, TODAY);

        assertThat(result).hasSize(2);                             // 섞이지 않고 2개
        assertThat(result.get(0).name()).isEqualTo("넷플릭스");     // 금액 큰 순
        assertThat(result.get(1).name()).isEqualTo("스포티파이");
        assertThat(result.get(0).months()).isEqualTo(3);           // 각각 3개월
        assertThat(result.get(1).months()).isEqualTo(3);
    }

    // ───────────── 제외되어야 하는 경우 ─────────────

    @Test
    @DisplayName("5. 같은 금액이 2번만 반복되면 제외")
    void exclude_onlyTwoTimes() {
        List<ExpenseRecord> records = List.of(
                sub(13500, "2026-10-15", "넷플릭스"),
                sub(13500, "2026-11-15", "넷플릭스"));

        assertThat(detector.detect(records, TODAY)).isEmpty();    // 3번 미만
    }

    @Test
    @DisplayName("6. 금액이 매번 조금씩 다른 일반 지출(점심값)은 제외")
    void exclude_lunchWithChangingAmount() {
        // Given: 간격은 한 달, 금액 차이도 30% 이내지만 금액이 2번 바뀜
        List<ExpenseRecord> records = List.of(
                food(9000, "2026-09-12"),
                food(9300, "2026-10-12"),
                food(8800, "2026-11-11"));

        assertThat(detector.detect(records, TODAY)).isEmpty();    // 금액 변화 2번 > 1번
    }

    @Test
    @DisplayName("7. 간격이 25일이거나 34일이면 제외")
    void exclude_wrongInterval() {
        List<ExpenseRecord> every25Days = List.of(                // 25일 간격 (26일보다 짧음)
                sub(5000, "2026-10-01", "A"),
                sub(5000, "2026-10-26", "A"),
                sub(5000, "2026-11-20", "A"));
        List<ExpenseRecord> every34Days = List.of(                // 34일 간격 (33일보다 김)
                sub(5000, "2026-09-01", "B"),
                sub(5000, "2026-10-05", "B"),
                sub(5000, "2026-11-08", "B"));

        assertThat(detector.detect(every25Days, TODAY)).isEmpty();
        assertThat(detector.detect(every34Days, TODAY)).isEmpty();
    }

    @Test
    @DisplayName("8. 3번 이어졌어도 마지막 결제 후 34일 동안 결제가 없으면 해지로 보고 제외")
    void exclude_cancelled() {
        // Given: 마지막 결제 10/30 → 오늘(12/3)까지 34일
        List<ExpenseRecord> records = List.of(
                sub(7900, "2026-08-30", "왓챠"),
                sub(7900, "2026-09-29", "왓챠"),
                sub(7900, "2026-10-30", "왓챠"));

        assertThat(detector.detect(records, TODAY)).isEmpty();    // 33일 초과
    }

    @Test
    @DisplayName("9. 중간에 한 달을 건너뛰면 사슬이 끊기고, 이후 2번만이면 제외")
    void exclude_skippedMonth() {
        // Given: 7·8월 → (9월 없음) → 10·11월. 8/15 → 10/15는 61일이라 이어 붙지 못함
        List<ExpenseRecord> records = List.of(
                sub(13500, "2026-07-15", "넷플릭스"),
                sub(13500, "2026-08-15", "넷플릭스"),
                sub(13500, "2026-10-15", "넷플릭스"),
                sub(13500, "2026-11-15", "넷플릭스"));

        assertThat(detector.detect(records, TODAY)).isEmpty();    // 두 사슬 모두 2번
    }

    @Test
    @DisplayName("10. 30% 넘게 오르면 다른 묶음으로 갈라져 제외")
    void exclude_increaseOverTolerance() {
        // Given: 10,000원 2번 → 14,000원(40% 인상)
        List<ExpenseRecord> records = List.of(
                sub(10000, "2026-09-15", "헬스장"),
                sub(10000, "2026-10-15", "헬스장"),
                sub(14000, "2026-11-15", "헬스장"));

        assertThat(detector.detect(records, TODAY)).isEmpty();    // [10000, 10000] + [14000]으로 갈라짐
    }
}