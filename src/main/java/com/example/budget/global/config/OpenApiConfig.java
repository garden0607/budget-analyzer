package com.example.budget.global.config;   // global/config 패키지 (SecurityConfig와 같은 곳)

import io.swagger.v3.oas.models.Components;                    // 보안 방식 등을 담는 묶음
import io.swagger.v3.oas.models.OpenAPI;                       // Swagger 문서 전체 설정
import io.swagger.v3.oas.models.info.Info;                     // 문서 제목, 버전, 설명
import io.swagger.v3.oas.models.security.SecurityRequirement;  // "이 문서의 API는 이 보안 방식을 쓴다"
import io.swagger.v3.oas.models.security.SecurityScheme;       // 보안 방식 정의 (JWT Bearer)
import org.springframework.context.annotation.Bean;            // 스프링에 객체 등록
import org.springframework.context.annotation.Configuration;   // 설정 클래스 표시

// Swagger 화면 설정: 제목 + 🔒 Authorize 버튼(JWT 입력칸)
@Configuration   // 스프링이 읽는 설정 클래스
public class OpenApiConfig {

    private static final String SCHEME_NAME = "JWT";   // 보안 방식에 붙일 이름 (아래 두 곳에서 같은 이름으로 연결)

    @Bean   // 이 설정을 스프링에 등록 → springdoc이 가져다 씀
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()                                           // 문서 맨 위에 보이는 정보
                        .title("소비 분석 앱 API")                          // 제목 ("OpenAPI definition" 대신)
                        .description("웹 가계부 및 소비 분석 서비스 API")      // 설명
                        .version("v1"))                                    // 버전
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))   // 모든 API에 "JWT" 보안 방식 적용 → 자물쇠 표시
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()     // "JWT"라는 보안 방식의 내용
                                .type(SecurityScheme.Type.HTTP)                  // HTTP 헤더로 보내는 방식
                                .scheme("bearer")                                // Authorization: Bearer ... 형식
                                .bearerFormat("JWT")));                          // 토큰 종류 표시 (설명용)
    }
}
// Authorize 버튼에 토큰만 넣으면 "Bearer "는 Swagger가 알아서 앞에 붙여 줌