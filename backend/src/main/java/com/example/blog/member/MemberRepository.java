package com.example.blog.member;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 메서드 이름만으로 Spring Data JPA가 쿼리를 만들어 준다.
    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);
}
