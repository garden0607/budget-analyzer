package com.example.budget.user.dto;   // user/dto 패키지

// 로그인 성공 시 서버가 화면에 돌려주는 데이터 상자
public record TokenResponse(
        String accessToken,   // 발급한 JWT (화면은 이걸 저장했다가 요청마다 헤더에 붙임)
        String nickname       // 로그인한 회원의 닉네임 (상단 메뉴에 "○○님" 표시용)
) {
}
// 화면에는 이런 JSON으로 전달됨:
// { "accessToken": "eyJhbGciOi...", "nickname": "정원" }