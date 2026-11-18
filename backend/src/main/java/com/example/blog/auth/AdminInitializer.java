package com.example.blog.auth;

import com.example.blog.member.Member;
import com.example.blog.member.MemberRepository;
import com.example.blog.member.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 서버가 뜰 때 한 번 실행되어, 설정에 적힌 관리자 계정이 없으면 만든다.
 * 회원가입 기능이 없으므로 이것이 관리자 계정을 만드는 유일한 방법이다.
 * 이미 있는 계정은 건드리지 않는다 (설정의 비밀번호를 바꿔도 기존 계정에는 반영되지 않는다).
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AdminProperties properties;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(
            AdminProperties properties,
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.username()) || !StringUtils.hasText(properties.password())) {
            if (memberRepository.count() == 0) {
                log.warn("관리자 계정이 없습니다. ADMIN_USERNAME, ADMIN_PASSWORD 환경변수를 설정하고 다시 시작하세요.");
            }
            return;
        }
        if (memberRepository.existsByUsername(properties.username())) {
            return;
        }

        // 비밀번호는 해시로 바꿔서 저장한다. 해시는 원문으로 되돌릴 수 없어서,
        // DB가 유출되어도 비밀번호 자체는 드러나지 않는다.
        String encodedPassword = passwordEncoder.encode(properties.password());
        memberRepository.save(new Member(properties.username(), encodedPassword, Role.ADMIN));
        log.info("관리자 계정을 만들었습니다: {}", properties.username());
    }
}
