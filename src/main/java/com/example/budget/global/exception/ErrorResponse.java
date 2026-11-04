package com.example.budget.global.exception;   // global/exception 패키지

// 오류가 났을 때 화면에 보내는 응답 상자
public record ErrorResponse(
        String code,      // 오류 이름 (화면이 상황을 구분할 때 사용, 예: "DUPLICATE_EMAIL")
        String message    // 안내 문구 (화면에 그대로 띄움, 예: "이미 가입된 이메일입니다.")
) {

    // ErrorCode로 응답 만들기 (BusinessException 처리할 때 사용)
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());   // enum 이름 + ErrorCode에 적은 문구
    }

    // ErrorCode + 직접 받은 문구로 응답 만들기 (입력값 검증 실패할 때 사용)
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);   // enum 이름 + DTO에 적은 문구
    }
}
// 화면에는 이런 JSON으로 전달됨:
// { "code": "DUPLICATE_EMAIL", "message": "이미 가입된 이메일입니다." }