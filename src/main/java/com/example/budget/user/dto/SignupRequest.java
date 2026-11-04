package com.example.budget.user.dto;   // 이 파일이 있는 위치(user/dto 패키지)

// 입력값 검증용 어노테이션들을 가져옴
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 회원가입할 때 화면이 보내는 데이터를 담는 상자(DTO)
// record: 값만 담는 상자를 짧게 만드는 문법 (생성자, getter 자동 생성, 값 변경 불가)
public record SignupRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")                      // 비어 있으면 안 됨
        @Email(message = "이메일 형식이 올바르지 않습니다.")                 // a@b.com 형식이어야 함
        @Size(max = 100, message = "이메일은 100자 이하로 입력해 주세요.")   // DB 컬럼 길이(100)에 맞춤
        String email,                                                     // 칸 1: 이메일

        @NotBlank(message = "비밀번호를 입력해 주세요.")                     // 비어 있으면 안 됨
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$",   // 영문 1개 이상 + 숫자 1개 이상, 8~20자
                message = "비밀번호는 영문과 숫자를 포함해 8~20자로 입력해 주세요.")
        String password,                                                  // 칸 2: 비밀번호

        @NotBlank(message = "비밀번호 확인을 입력해 주세요.")                 // 비어 있으면 안 됨
        String passwordConfirm,                                           // 칸 3: 비밀번호 확인 (일치 여부는 Service에서 비교)

        @NotBlank(message = "닉네임을 입력해 주세요.")                       // 비어 있으면 안 됨
        @Size(min = 2, max = 10, message = "닉네임은 2~10자로 입력해 주세요.") // 설계서 규칙: 2~10자
        String nickname                                                   // 칸 4: 닉네임
) {
}
// 규칙을 어기면 MethodArgumentNotValidException이 발생하고,
// GlobalExceptionHandler가 위 message 문구를 꺼내 400 응답으로 보냄