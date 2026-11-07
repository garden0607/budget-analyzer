package com.example.budget.transaction;   // transaction 패키지

import com.example.budget.category.TransactionType;                // ★ 추가: 수입/지출 enum (검색 조건)
import com.example.budget.transaction.dto.TransactionPageResponse; // ★ 추가: 조회 결과 상자
import com.example.budget.transaction.dto.TransactionRequest;      // 등록·수정 요청 상자
import com.example.budget.transaction.dto.TransactionResponse;     // 응답 상자
import jakarta.validation.Valid;                                   // DTO 검증 실행
import lombok.RequiredArgsConstructor;                             // final 필드 생성자 자동 생성
import org.springframework.format.annotation.DateTimeFormat;       // ★ 추가: 주소의 날짜 글자 → LocalDate 변환 형식
import org.springframework.http.HttpStatus;                        // 상태 코드 목록
import org.springframework.http.ResponseEntity;                    // 상태 코드 + 내용 응답
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 토큰의 회원 id 꺼내기
import org.springframework.web.bind.annotation.*;                  // @GetMapping 등 전체

import java.time.LocalDate;   // ★ 추가: 날짜 타입

@RestController                      // API 창구, 돌려주는 값은 자동으로 JSON
@RequestMapping("/api/transactions") // 이 클래스의 모든 주소 앞에 /api/transactions
@RequiredArgsConstructor             // final 필드 생성자 자동 생성
public class TransactionController {

    private final TransactionService transactionService;   // 실제 처리는 Service에 맡김

    // ★ 추가: 조회 GET /api/transactions?startDate=2026-11-01&endDate=2026-11-30&categoryId=1&type=EXPENSE&page=0
    @GetMapping
    public ResponseEntity<TransactionPageResponse> search(
            @AuthenticationPrincipal Long userId,                                   // 토큰에서 꺼낸 회원 id
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,     // 시작일 "2026-11-01" (안 보내면 null → 이번 달 1일)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,       // 종료일 (안 보내면 null → 이번 달 말일)
            @RequestParam(required = false) Long categoryId,                        // 카테고리 (안 보내면 전체)
            @RequestParam(required = false) TransactionType type,                   // 수입/지출 (안 보내면 전체)
            @RequestParam(defaultValue = "0") int page) {                           // 페이지 번호 (안 보내면 0 = 첫 페이지)
        return ResponseEntity.ok(
                transactionService.search(userId, startDate, endDate, categoryId, type, page));   // 200 + 조회 결과
    }

    // 등록: POST /api/transactions
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal Long userId,                        // 회원 id
            @Valid @RequestBody TransactionRequest request) {            // JSON → 상자 + 검증
        TransactionResponse response = transactionService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);   // 201
    }

    // 수정: PUT /api/transactions/3
    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> update(
            @AuthenticationPrincipal Long userId,                        // 회원 id
            @PathVariable Long transactionId,                            // 주소의 3
            @Valid @RequestBody TransactionRequest request) {            // 등록과 같은 상자, 같은 검증
        return ResponseEntity.ok(transactionService.update(userId, transactionId, request));   // 200
    }

    // 삭제: DELETE /api/transactions/3
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,                        // 회원 id
            @PathVariable Long transactionId) {                          // 주소의 3
        transactionService.delete(userId, transactionId);
        return ResponseEntity.noContent().build();                       // 204
    }
}