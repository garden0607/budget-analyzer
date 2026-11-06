package com.example.budget.transaction;   // transaction 패키지

import com.example.budget.transaction.dto.TransactionRequest;    // 등록·수정 요청 상자
import com.example.budget.transaction.dto.TransactionResponse;   // 응답 상자
import jakarta.validation.Valid;                                 // DTO 검증 실행
import lombok.RequiredArgsConstructor;                           // final 필드 생성자 자동 생성
import org.springframework.http.HttpStatus;                      // 상태 코드 목록
import org.springframework.http.ResponseEntity;                  // 상태 코드 + 내용 응답
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 토큰의 회원 id 꺼내기
import org.springframework.web.bind.annotation.*;                // @PostMapping 등 전체

@RestController                      // API 창구, 돌려주는 값은 자동으로 JSON
@RequestMapping("/api/transactions") // 이 클래스의 모든 주소 앞에 /api/transactions
@RequiredArgsConstructor             // final 필드 생성자 자동 생성
public class TransactionController {

    private final TransactionService transactionService;   // 실제 처리는 Service에 맡김

    // 등록: POST /api/transactions
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal Long userId,                        // 토큰에서 꺼낸 회원 id
            @Valid @RequestBody TransactionRequest request) {            // JSON → 상자 + 검증 (금액 1~1,000만 원, 메모 100자 등)
        TransactionResponse response = transactionService.create(userId, request);   // 등록하고 결과 받기
        return ResponseEntity.status(HttpStatus.CREATED).body(response);              // 201 + 등록된 거래
    }

    // 수정: PUT /api/transactions/3
    @PutMapping("/{transactionId}")                                      // {transactionId} 자리에 숫자
    public ResponseEntity<TransactionResponse> update(
            @AuthenticationPrincipal Long userId,                        // 회원 id
            @PathVariable Long transactionId,                            // 주소의 3을 꺼냄
            @Valid @RequestBody TransactionRequest request) {            // 등록과 같은 상자, 같은 검증
        return ResponseEntity.ok(transactionService.update(userId, transactionId, request));   // 200 + 수정된 거래
    }

    // 삭제: DELETE /api/transactions/3
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,                        // 회원 id
            @PathVariable Long transactionId) {                          // 주소의 3을 꺼냄
        transactionService.delete(userId, transactionId);                // 삭제 처리
        return ResponseEntity.noContent().build();                       // 204 No Content
    }
}