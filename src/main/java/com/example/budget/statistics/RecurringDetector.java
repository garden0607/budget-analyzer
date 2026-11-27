package com.example.budget.statistics;   // statistics 패키지 (StatisticsService 옆)

import com.example.budget.statistics.dto.ExpenseRecord;      // 입력: 지출 거래 한 건
import com.example.budget.statistics.dto.RecurringResponse;  // 출력: 정기 지출 한 줄(Item)
import org.springframework.stereotype.Component;             // 스프링이 관리하는 부품으로 등록

import java.time.LocalDate;            // 날짜
import java.time.YearMonth;            // 연·월 ("2026-10")
import java.time.temporal.ChronoUnit;  // 두 날짜 사이 일수 계산
import java.util.ArrayList;            // 크기가 늘어나는 목록
import java.util.Comparator;           // 정렬 기준
import java.util.List;                 // 목록

// 정기 지출 자동 탐지 알고리즘 (직접 설계·구현한 핵심 알고리즘)
// 거래 목록을 받아 "한 달 간격으로 이어진 사슬"을 만들고, 조건을 통과한 사슬을 정기 지출로 판단한다
// DB를 쓰지 않는 순수 계산이라 단위 테스트에서 new RecurringDetector()로 바로 검증할 수 있음
@Component
public class RecurringDetector {

    // ───── 기준값 (설계서 3장 "기준값과 근거") ─────
    static final int MIN_INTERVAL_DAYS = 26;      // 결제 간격 최소: 28일(2월) - 2일(결제일 밀림·입력 오차)
    static final int MAX_INTERVAL_DAYS = 33;      // 결제 간격 최대: 31일 + 2일
    static final double AMOUNT_TOLERANCE = 0.30;  // 금액 허용 범위 30%: 실제 구독료 1회 인상 폭(26~27%) + 여유
    static final int MIN_COUNT = 3;               // 최소 반복 횟수: 간격 2개가 모두 맞아야 우연이 아님
    static final int MAX_AMOUNT_CHANGES = 1;      // 금액 변화 허용 횟수: 구독은 한 번 오르면 유지, 일반 지출은 매번 다름
    static final int RECENT_DAYS = 33;            // 최근 결제 기준: 마지막 결제 후 33일이 지나면 해지로 봄

    // ───── 알고리즘 시작점: 거래 목록 → 정기 지출 목록 ─────
    public List<RecurringResponse.Item> detect(List<ExpenseRecord> records, LocalDate today) {

        // [1단계] 날짜순 정렬 (원본 목록은 건드리지 않고 정렬된 새 목록을 만듦)
        List<ExpenseRecord> sorted = records.stream()
                .sorted(Comparator.comparing(ExpenseRecord::date))   // 오래된 거래부터
                .toList();

        // [2단계] 사슬 만들기: 거래를 하나씩 보며 이어 붙일 사슬을 찾고, 없으면 새 사슬 시작
        List<List<ExpenseRecord>> chains = new ArrayList<>();         // 사슬 목록 (사슬 = 이어진 거래 목록)
        for (ExpenseRecord record : sorted) {
            List<ExpenseRecord> best = findBestChain(chains, record); // 붙을 수 있는 사슬 중 가장 알맞은 것
            if (best != null) {
                best.add(record);                                     // 그 사슬 맨 뒤에 이어 붙이기
            } else {
                List<ExpenseRecord> chain = new ArrayList<>();        // 붙을 곳이 없으면
                chain.add(record);                                    // 이 거래로 새 사슬 시작
                chains.add(chain);
            }
        }

        // [3단계] 조건 검사 → [4단계] 결과 만들기
        return chains.stream()
                .filter(chain -> isRecurring(chain, today))            // 조건을 통과한 사슬만
                .map(this::toItem)                                     // 결과 한 줄로 바꾸고
                .sorted(Comparator.comparingInt(RecurringResponse.Item::amount).reversed()) // 금액 큰 순
                .toList();
    }

    // ───── [2단계] 이 거래를 이어 붙일 사슬 찾기 ─────
    // 조건: 같은 카테고리 + 사슬의 마지막 거래와 26~33일 차이 + 금액 차이 30% 이내
    // 여러 사슬이 가능하면 금액 차이가 가장 작은 사슬 (비슷한 구독 두 개가 섞이지 않게)
    private List<ExpenseRecord> findBestChain(List<List<ExpenseRecord>> chains, ExpenseRecord record) {
        List<ExpenseRecord> best = null;          // 지금까지 찾은 가장 알맞은 사슬
        double bestDiff = Double.MAX_VALUE;       // 그 사슬과의 금액 차이 (처음엔 가장 큰 값)

        for (List<ExpenseRecord> chain : chains) {
            ExpenseRecord last = chain.get(chain.size() - 1);         // 사슬의 마지막 거래하고만 비교

            if (!last.categoryId().equals(record.categoryId())) continue; // 카테고리가 다르면 건너뜀

            long days = ChronoUnit.DAYS.between(last.date(), record.date()); // 마지막 거래와 며칠 차이
            if (days < MIN_INTERVAL_DAYS || days > MAX_INTERVAL_DAYS) continue; // 한 달 간격이 아니면 건너뜀

            double diff = amountDiffRate(last.amount(), record.amount()); // 금액 차이 비율
            if (diff > AMOUNT_TOLERANCE) continue;                    // 30% 넘게 다르면 건너뜀

            if (diff < bestDiff) {                                    // 지금까지보다 금액이 더 비슷하면
                best = chain;                                         // 이 사슬을 후보로
                bestDiff = diff;
            }
        }
        return best;                                                  // 없으면 null → 새 사슬 시작
    }

    // 금액 차이 비율: 13,500 → 17,000이면 (17,000 - 13,500) ÷ 13,500 = 약 0.26 (26%)
    private double amountDiffRate(int before, int after) {
        return Math.abs(after - before) / (double) before;            // 오르든 내리든 크기만 봄 (금액은 1원 이상이라 0으로 나누지 않음)
    }

    // ───── [3단계] 이 사슬이 정기 지출인지 검사 ─────
    private boolean isRecurring(List<ExpenseRecord> chain, LocalDate today) {
        if (chain.size() < MIN_COUNT) return false;                   // 3번 미만 → 우연일 수 있음

        if (countAmountChanges(chain) > MAX_AMOUNT_CHANGES) return false; // 금액이 자주 바뀜 → 일반 지출 (점심값 등)

        LocalDate lastDate = chain.get(chain.size() - 1).date();      // 마지막 결제일
        return ChronoUnit.DAYS.between(lastDate, today) <= RECENT_DAYS; // 33일 넘게 결제가 없으면 해지로 봄
    }

    // 사슬 안에서 금액이 바뀐 횟수: 13,500 → 13,500 → 17,000 → 17,000 이면 1번
    private int countAmountChanges(List<ExpenseRecord> chain) {
        int changes = 0;
        for (int i = 1; i < chain.size(); i++) {                      // 두 번째 거래부터
            if (chain.get(i).amount() != chain.get(i - 1).amount()) { // 바로 앞 거래와 금액이 다르면
                changes++;
            }
        }
        return changes;
    }

    // ───── [4단계] 사슬 → 결과 한 줄 (판단 이유의 재료 포함) ─────
    private RecurringResponse.Item toItem(List<ExpenseRecord> chain) {
        ExpenseRecord last = chain.get(chain.size() - 1);             // 가장 최근 거래

        String name = (last.memo() != null && !last.memo().isBlank()) // 메모가 있으면 메모("넷플릭스")
                ? last.memo()
                : last.categoryName();                                // 없으면 카테고리 이름("구독")

        Integer previousAmount = null;                                // 인상 전 금액 (안 바뀌었으면 null)
        String changedMonth = null;                                   // 바뀐 달 (안 바뀌었으면 null)
        for (int i = chain.size() - 1; i >= 1; i--) {                 // 뒤에서부터 보면서
            if (chain.get(i).amount() != chain.get(i - 1).amount()) { // 금액이 바뀐 지점을 찾으면
                previousAmount = chain.get(i - 1).amount();           // 바뀌기 전 금액
                changedMonth = YearMonth.from(chain.get(i).date()).toString(); // 바뀐 달 "2026-10"
                break;                                                // 가장 최근 변화 하나만
            }
        }

        return new RecurringResponse.Item(
                name,
                last.categoryName(),
                last.amount(),                    // 최근 금액 (인상됐으면 오른 금액)
                chain.size(),                     // 연속 개월 수
                last.date().getDayOfMonth(),      // 결제일 (최근 거래의 "일")
                previousAmount,
                changedMonth);
    }
}