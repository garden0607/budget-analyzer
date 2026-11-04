package com.example.budget;   // 프로젝트 최상위 패키지 (모든 패키지가 이 아래에 있음)

import org.springframework.boot.SpringApplication;                          // 스프링 부트 실행 도구
import org.springframework.boot.autoconfigure.SpringBootApplication;        // 스프링 부트 자동 설정
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;    // 생성·수정 시간 자동 기록 기능

// 프로그램의 시작점 (▶ 실행 버튼을 누르면 여기서 시작)
@EnableJpaAuditing        // @CreatedDate, @LastModifiedDate가 동작하게 켜기 (BaseCreatedEntity, BaseTimeEntity용)
@SpringBootApplication    // 자동 설정 + 이 패키지 아래의 @Component, @Service 등을 찾아서 등록
public class BudgetApplication {

    public static void main(String[] args) {                      // 자바 프로그램 시작 메서드
        SpringApplication.run(BudgetApplication.class, args);    // 스프링 부트 서버 실행 (내장 톰캣 8080 포트)
    }
}