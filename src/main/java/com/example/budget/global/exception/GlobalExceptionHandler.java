package com.example.budget.global.exception;   // global/exception 패키지

import lombok.extern.slf4j.Slf4j;                                       // 로그 도구 (log.error 등)
import org.springframework.http.ResponseEntity;                         // 상태 코드 + 내용을 함께 담는 응답
import org.springframework.http.converter.HttpMessageNotReadableException; // JSON을 못 읽었을 때 나는 예외
import org.springframework.validation.FieldError;                       // 검증에 실패한 칸 하나의 정보
import org.springframework.web.bind.MethodArgumentNotValidException;    // @Valid 검증 실패 시 나는 예외
import org.springframework.web.bind.annotation.ExceptionHandler;        // "이 예외는 이 메서드가 처리" 표시
import org.springframework.web.bind.annotation.RestControllerAdvice;    // 모든 Controller의 예외를 한곳에서 처리

// Controller·Service에서 난 예외를 받아서 { code, message } JSON으로 바꿔 응답하는 곳
@Slf4j                   // log 변수 자동 생성
@RestControllerAdvice    // 모든 Controller에 적용되는 예외 처리 클래스로 등록
public class GlobalExceptionHandler {

    // 1. 우리가 직접 던진 예외 (이메일 중복, 데이터 없음 등)
    @ExceptionHandler(BusinessException.class)                                   // BusinessException이 오면 이 메서드 실행
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();                                       // 예외 안에 담긴 ErrorCode 꺼내기
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code)); // 그 오류의 상태 코드(409 등) + 응답 상자
    }

    // 2. 입력값 검증 실패 (@Valid: 금액 0원, 이메일 형식 오류 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)                     // @Valid 검사에 걸리면 이 메서드 실행
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();            // 실패한 칸 중 첫 번째 가져오기
        String message = (fieldError != null) ? fieldError.getDefaultMessage()   // 있으면 DTO에 적은 문구 사용
                : ErrorCode.INVALID_INPUT.getMessage();                          // 없으면 기본 문구 "입력값이 올바르지 않습니다."
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())        // 400
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message));       // code는 INVALID_INPUT, 문구는 위에서 고른 것
    }

    // 3. JSON 형식이 잘못된 요청 (괄호 빠짐, 숫자 칸에 글자 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)                     // JSON을 DTO로 못 바꾸면 이 메서드 실행
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())        // 400
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));                // 기본 문구로 응답
    }

    // 4. 예상하지 못한 나머지 모든 예외
    @ExceptionHandler(Exception.class)                                           // 위에서 안 잡힌 모든 예외가 여기로
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("예상하지 못한 오류", e);                                       // 원인을 찾을 수 있게 콘솔에 자세히 기록
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())       // 500
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));               // 화면에는 내부 내용 숨기고 "서버 오류" 문구만
    }
}
// 인증 실패(401)는 Controller에 오기 전 필터 단계에서 나서 여기서 못 잡음 → SecurityConfig에서 따로 처리