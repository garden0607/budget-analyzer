package com.example.budget.user;   // user 패키지

import com.example.budget.global.BaseCreatedEntity;   // 생성일시 자동 기록 부모 클래스
import jakarta.persistence.*;                         // JPA 어노테이션 전체 (@Entity, @Id, @Column ...)
import lombok.AccessLevel;                            // 접근 범위 지정 (PROTECTED 등)
import lombok.Builder;                                // 빌더 패턴 자동 생성
import lombok.Getter;                                 // getter 자동 생성
import lombok.NoArgsConstructor;                      // 기본 생성자 자동 생성

// 회원 엔티티 = DB의 users 테이블과 연결되는 클래스
@Getter                                               // getId(), getEmail() ... 자동 생성 (setter는 일부러 안 만듦)
@NoArgsConstructor(access = AccessLevel.PROTECTED)    // 빈 생성자: JPA가 객체 만들 때 필요, 밖에서 new User()는 못 하게 막음
@Entity                                               // 이 클래스는 DB 테이블과 연결됨
@Table(name = "users")                                // 테이블 이름 users (user는 MySQL 예약어라 피함)
public class User extends BaseCreatedEntity {         // created_at 칸을 물려받음

    @Id                                                    // 기본키(PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // id는 DB가 1, 2, 3 ... 자동 증가로 정해 줌
    private Long id;                                       // 회원 id (JWT에 담기는 값)

    @Column(nullable = false, unique = true, length = 100) // 빈 값 불가, 중복 불가(UNIQUE), 최대 100자
    private String email;                                  // 이메일 (로그인 아이디)

    @Column(nullable = false, length = 60)                 // 빈 값 불가, 60자 (BCrypt 결과 길이)
    private String password;                               // 암호화된 비밀번호

    @Column(nullable = false, length = 10)                 // 빈 값 불가, 최대 10자
    private String nickname;                               // 닉네임

    @Builder                                                          // User.builder().email(..).build() 방식으로 만들게 해 줌
    private User(String email, String password, String nickname) {    // private: 빌더로만 생성 가능
        this.email = email;                                           // 받은 이메일 저장
        this.password = password;                                     // 받은 (암호화된) 비밀번호 저장
        this.nickname = nickname;                                     // 받은 닉네임 저장
    }
}
// id는 DB가 정하고, created_at은 자동 기록되니까 빌더에서 받지 않음