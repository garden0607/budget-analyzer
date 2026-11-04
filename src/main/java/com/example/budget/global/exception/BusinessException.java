package com.example.budget.global.exception;   // global/exception 패키지

import lombok.Getter;   // getter 자동 생성 (Lombok)

// Service에서 "규칙 위반" 상황을 알릴 때 던지는 우리만의 예외
@Getter                                                 // getErrorCode() 자동 생성 (GlobalExceptionHandler가 꺼내 씀)
public class BusinessException extends RuntimeException {   // 자바 기본 예외를 물려받음 (throws 선언 없이 던질 수 있음)

    private final ErrorCode errorCode;   // 어떤 오류인지 담는 칸 (상태 코드, 문구까지 알 수 있음)

    public BusinessException(ErrorCode errorCode) {   // 생성자: 던질 때 ErrorCode를 받음
        super(errorCode.getMessage());                // 부모(RuntimeException)에게 문구 전달 → 로그에 이 문구가 찍힘
        this.errorCode = errorCode;                   // 받은 ErrorCode를 칸에 저장
    }
}
// 사용 예 (AuthService):
// throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);