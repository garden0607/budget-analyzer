package com.example.budget.budget;   // 테스트할 BudgetStatus와 같은 패키지

import org.junit.jupiter.api.DisplayName;   // 테스트 이름을 한글로 보여 주기
import org.junit.jupiter.api.Test;          // "이 메서드는 테스트다" 표시

import static org.assertj.core.api.Assertions.assertThat;   // "결과는 이래야 한다" 검사 도구

// BudgetStatus의 상태 판정(of)과 위험도 비교(isMoreSevereThan)를 확인하는 테스트
class BudgetStatusTest {

    @Test
    @DisplayName("사용률 79%는 정상")
    void rate79IsNormal() {
        assertThat(BudgetStatus.of(79)).isEqualTo(BudgetStatus.NORMAL);   // of(79)의 결과는 NORMAL이어야 한다
    }

    @Test
    @DisplayName("사용률 80%부터 경고")
    void rate80IsWarning() {
        assertThat(BudgetStatus.of(80)).isEqualTo(BudgetStatus.WARNING);  // 경계값: 80은 경고
    }

    @Test
    @DisplayName("사용률 99%는 아직 경고")
    void rate99IsWarning() {
        assertThat(BudgetStatus.of(99)).isEqualTo(BudgetStatus.WARNING);  // 100 직전까지는 경고
    }

    @Test
    @DisplayName("사용률 100%부터 초과")
    void rate100IsExceeded() {
        assertThat(BudgetStatus.of(100)).isEqualTo(BudgetStatus.EXCEEDED); // 경계값: 100은 초과
    }

    @Test
    @DisplayName("사용률 120%도 초과 (큰 기준부터 검사하는지)")
    void rate120IsExceeded() {
        assertThat(BudgetStatus.of(120)).isEqualTo(BudgetStatus.EXCEEDED); // if 순서가 바뀌면 WARNING이 나와서 실패함
    }

    @Test
    @DisplayName("더 위험한 쪽으로 바뀌면 true")
    void moreSevere() {
        assertThat(BudgetStatus.WARNING.isMoreSevereThan(BudgetStatus.NORMAL)).isTrue();    // 정상 → 경고
        assertThat(BudgetStatus.EXCEEDED.isMoreSevereThan(BudgetStatus.WARNING)).isTrue();  // 경고 → 초과
        assertThat(BudgetStatus.EXCEEDED.isMoreSevereThan(BudgetStatus.NORMAL)).isTrue();   // 정상 → 초과
    }

    @Test
    @DisplayName("같거나 덜 위험해지면 false")
    void notMoreSevere() {
        assertThat(BudgetStatus.WARNING.isMoreSevereThan(BudgetStatus.WARNING)).isFalse();  // 경고 → 경고 (중복 방지)
        assertThat(BudgetStatus.WARNING.isMoreSevereThan(BudgetStatus.EXCEEDED)).isFalse(); // 초과 → 경고 (위험 감소)
        assertThat(BudgetStatus.NORMAL.isMoreSevereThan(BudgetStatus.EXCEEDED)).isFalse();  // 초과 → 정상
    }
}