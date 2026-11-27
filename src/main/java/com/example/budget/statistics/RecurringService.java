package com.example.budget.statistics;   // statistics 패키지

import com.example.budget.statistics.dto.ExpenseRecord;      // 알고리즘 입력
import com.example.budget.statistics.dto.RecurringResponse;  // 알고리즘 결과
import com.example.budget.transaction.TransactionRepository; // 거래 DB 작업
import com.example.budget.user.UserRepository;               // ★ 추가: 회원 DB 작업 (제외 저장 시 회원 연결)
import lombok.RequiredArgsConstructor;                       // final 필드 생성자 자동 생성
import org.springframework.stereotype.Service;               // Service 표시
import org.springframework.transaction.annotation.Transactional; // 트랜잭션

import java.time.LocalDate;   // 날짜
import java.time.ZoneId;      // 시간대
import java.util.List;        // 목록
import java.util.Set;         // ★ 추가: 중복 없는 모음 (제외 목록 빠르게 확인)
import java.util.stream.Collectors; // ★ 추가: 스트림 결과를 Set으로 모으기

// 정기 지출 탐지: DB에서 거래를 꺼내 알고리즘(RecurringDetector)에 넘기고,
// 사용자가 "아니에요"로 제외한 항목을 빼서 돌려줌
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본은 조회만
public class RecurringService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");   // "오늘"은 한국 시간 기준
    private static final int LOOKBACK_MONTHS = 12;                  // 최근 12개월 거래만 봄

    private final TransactionRepository transactionRepository;      // 거래 조회
    private final RecurringDetector recurringDetector;              // 알고리즘 (판단만 담당)
    private final RecurringExclusionRepository exclusionRepository; // ★ 추가: 제외 목록
    private final UserRepository userRepository;                    // ★ 추가: 제외 저장 시 회원 연결

    // ───────────── 정기 지출 조회 ─────────────
    public RecurringResponse getRecurring(Long userId) {
        LocalDate today = LocalDate.now(KOREA);                      // 오늘

        List<ExpenseRecord> records = transactionRepository
                .findExpensesBetween(userId, today.minusMonths(LOOKBACK_MONTHS), today) // 최근 12개월 지출
                .stream()
                .map(t -> new ExpenseRecord(                         // 엔티티 → 알고리즘 입력으로 변환
                        t.getCategory().getId(),
                        t.getCategory().getName(),
                        t.getAmount(),
                        t.getTransactionDate(),
                        t.getMemo()))
                .toList();

        List<RecurringResponse.Item> detected = recurringDetector.detect(records, today); // ★ 알고리즘 실행 (판단)

        Set<String> excluded = exclusionRepository.findByUserId(userId).stream()        // 내 제외 목록을
                .map(e -> key(e.getCategoryId(), e.getName()))                          // "카테고리id:이름" 이름표로 바꿔
                .collect(Collectors.toSet());                                           // Set에 모음 (있는지 빠르게 확인)

        List<RecurringResponse.Item> items = detected.stream()
                .filter(item -> !excluded.contains(key(item.categoryId(), item.name()))) // 제외한 묶음은 빼기 (사용자 판단 반영)
                .toList();

        long monthlyTotal = items.stream()
                .mapToLong(RecurringResponse.Item::amount)            // 남은 정기 지출의 최근 금액 합계
                .sum();

        return new RecurringResponse(items, monthlyTotal);
    }

    // ───────────── "아니에요": 제외 목록에 저장 ─────────────
    @Transactional   // 저장하므로 읽기 전용 해제
    public void exclude(Long userId, Long categoryId, String name) {
        if (exclusionRepository.existsByUserIdAndCategoryIdAndName(userId, categoryId, name)) {
            return;                                                   // 이미 제외했으면 그대로 (두 번 눌러도 같은 결과)
        }
        exclusionRepository.save(RecurringExclusion.builder()
                .user(userRepository.getReferenceById(userId))       // 회원 연결 (SELECT 없이 id만으로 참조)
                .categoryId(categoryId)
                .name(name)
                .build());
    }

    // 묶음을 알아보는 이름표: 카테고리 id + 이름 (금액은 인상되면 바뀌니까 넣지 않음)
    private String key(Long categoryId, String name) {
        return categoryId + ":" + name;                               // 예: "7:점심"
    }
}