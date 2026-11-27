package com.example.budget.statistics;   // statistics 패키지 (StatisticsService와 같은 곳)

import com.example.budget.statistics.dto.CategoryRatioResponse;   // 카테고리 비율 응답 상자
import com.example.budget.statistics.dto.ComparisonResponse;      // 전월 대비 응답 상자
import com.example.budget.statistics.dto.SummaryResponse;         // 월 합계 응답 상자
import lombok.RequiredArgsConstructor;                            // final 필드를 받는 생성자 자동 생성
import org.springframework.format.annotation.DateTimeFormat;      // 주소의 "2026-11" 글자를 YearMonth로 바꾸는 형식 지정
import org.springframework.http.ResponseEntity;                   // 상태 코드(200 등) + 내용을 담는 봉투
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 토큰에서 회원 id 꺼내기
import org.springframework.web.bind.annotation.GetMapping;        // GET 요청 주소 연결
import org.springframework.web.bind.annotation.RequestMapping;    // 클래스 공통 주소
import org.springframework.web.bind.annotation.RequestParam;      // 주소 뒤 ?month=... 값 받기
import org.springframework.web.bind.annotation.RestController;    // API 창구 표시
import com.example.budget.statistics.dto.RecurringResponse;   // 정기 지출 탐지 결과

import java.time.YearMonth;   // 연·월 (2026-11)

// 통계 API 창구: 화면이 부를 주소 3개를 열고, 계산은 StatisticsService에 맡김
@RestController                    // API 창구. 돌려주는 값을 자동으로 JSON으로 바꿔 줌
@RequestMapping("/api/statistics") // 이 클래스의 모든 주소 앞에 /api/statistics가 붙음 (3개 주소 공통)
@RequiredArgsConstructor           // 아래 final 필드를 스프링이 넣어 주는 생성자 자동 생성 (의존성 주입)
public class StatisticsController {

    private final StatisticsService statisticsService;   // 실제 계산 담당. Controller는 받고 넘기고 돌려주기만 함
    private final RecurringService recurringService;           // 정기 지출 탐지 (알고리즘 연결)

    // ───────────── 월 합계 (FR-13): GET /api/statistics/summary?month=2026-11 ─────────────
    @GetMapping("/summary")                                               // GET 방식 + /summary → 조회만 하니까 GET
    public ResponseEntity<SummaryResponse> getSummary(                    // 200 + 월 합계 상자를 돌려줌
                                                                          @AuthenticationPrincipal Long userId,                         // 토큰에서 꺼낸 회원 id → 내 거래로만 계산 (남의 id로 못 바꿈)
                                                                          @RequestParam(required = false)                               // ?month= 를 안 보내도 오류 없이 null
                                                                          @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {       // "2026-11" → YearMonth (2026-13이면 400)
        return ResponseEntity.ok(statisticsService.getSummary(userId, month));   // Service가 계산한 결과를 200으로 (null이면 이번 달)
    }

    // ───────────── 카테고리별 비율 (FR-14): GET /api/statistics/category-ratio?month=2026-11 ─────────────
    @GetMapping("/category-ratio")                                        // GET 방식 + /category-ratio → 원형 차트 데이터
    public ResponseEntity<CategoryRatioResponse> getCategoryRatio(        // 200 + 카테고리별 금액·비율 상자
                                                                          @AuthenticationPrincipal Long userId,                         // 토큰의 회원 id (내 지출만)
                                                                          @RequestParam(required = false)                               // 월은 선택
                                                                          @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {       // "2026-11" → YearMonth
        return ResponseEntity.ok(statisticsService.getCategoryRatio(userId, month));   // 금액 큰 순, 소수점 첫째 자리 비율
    }

    // ───────────── 전월 대비 (FR-15): GET /api/statistics/comparison?month=2026-11 ─────────────
    @GetMapping("/comparison")                                            // GET 방식 + /comparison → 막대 차트 데이터
    public ResponseEntity<ComparisonResponse> getComparison(              // 200 + 카테고리별 증감 상자
                                                                          @AuthenticationPrincipal Long userId,                         // 토큰의 회원 id (내 지출만)
                                                                          @RequestParam(required = false)                               // 월은 선택 (지난달은 Service가 자동 계산)
                                                                          @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {       // "2026-11" → YearMonth
        return ResponseEntity.ok(statisticsService.getComparison(userId, month));   // 지난달 0원이면 증감률 null → 화면에 "신규"
    }
    // 정기 지출 탐지: GET /api/statistics/recurring (항상 오늘 기준, 최근 12개월 거래로 판단)
    @GetMapping("/recurring")
    public ResponseEntity<RecurringResponse> getRecurring(
            @AuthenticationPrincipal Long userId) {                    // 토큰에서 꺼낸 회원 id → 내 거래만
        return ResponseEntity.ok(recurringService.getRecurring(userId)); // 200 + 정기 지출 목록 + 월 합계
    }
}
// 세 주소 모두 /api/auth/** 가 아니라서 토큰이 없으면 401 (SecurityConfig 규칙)