package com.example.budget.user;   // user 패키지

import org.springframework.data.jpa.repository.JpaRepository;   // 기본 저장·조회 기능 제공

import java.util.Optional;   // 있을 수도 없을 수도 있는 값

// 회원 DB 작업 (JpaRepository<엔티티, id 타입>)
public interface UserRepository extends JpaRepository<User, Long> {   // save, findById, delete 등은 자동으로 생김

    Optional<User> findByEmail(String email);   // 이메일로 회원 찾기 → 로그인 (없으면 빈 Optional)

    boolean existsByEmail(String email);        // 이메일이 이미 있는지 → 회원가입 중복 확인
}