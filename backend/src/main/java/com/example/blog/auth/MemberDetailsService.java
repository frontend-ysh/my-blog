package com.example.blog.auth;

import com.example.blog.member.MemberRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스프링 시큐리티가 로그인할 때 "이 아이디의 계정 정보를 달라"고 부르는 곳.
 * 우리 Member를 시큐리티가 이해하는 UserDetails로 바꿔 준다.
 * 비밀번호 비교는 여기서 하지 않고 시큐리티가 한다.
 */
@Service
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public MemberDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return memberRepository.findByUsername(username)
                .map(member -> User.withUsername(member.getUsername())
                        .password(member.getPassword())
                        // roles("ADMIN")은 권한 이름 "ROLE_ADMIN"을 만든다.
                        .roles(member.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
