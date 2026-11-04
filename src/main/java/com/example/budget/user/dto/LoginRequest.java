package com.example.budget.user.dto;   // user/dto 패키지

import jakarta.validation.constraints.NotBlank;   // 비어 있으면 안 된다는 검증 어노테이션

// 로그인할 때 화면이 보내는 데이터 상자
public record LoginRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")     // 비어 있으면 안 됨
        String email,                                    // 칸 1: 이메일

        @NotBlank(message = "비밀번호를 입력해 주세요.")   // 비어 있으면 안 됨
        String password                                  // 칸 2: 비밀번호
) {
}
// 로그인은 형식 검사(이메일 형식, 8~20자 등)를 하지 않고 "비었는지"만 확인함
// → 맞는지 틀렸는지는 Service에서 DB와 비교해서 판단