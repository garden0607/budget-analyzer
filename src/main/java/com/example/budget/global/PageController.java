package com.example.budget.global;                    // 이 파일이 속한 패키지(global)

import org.springframework.stereotype.Controller;     // 화면을 돌려주는 컨트롤러 표시
import org.springframework.web.bind.annotation.GetMapping; // GET 요청 주소 연결

@Controller                                           // JSON이 아니라 화면 이름을 돌려주는 컨트롤러
public class PageController {                         // 화면 주소 → HTML 파일 연결만 담당

    @GetMapping("/")                                  // 첫 화면 주소
    public String home() {
        return "index";                               // templates/index.html을 보여 줌 (오늘 7단계에서 임시로 만듦)
    }

    @GetMapping("/login")                             // 로그인 화면 주소 (S-01)
    public String login() {
        return "login";                               // templates/login.html (3일차에 만듦)
    }

    @GetMapping("/signup")                            // 회원가입 화면 주소 (S-02)
    public String signup() {
        return "signup";                              // templates/signup.html (3일차에 만듦)
    }
}