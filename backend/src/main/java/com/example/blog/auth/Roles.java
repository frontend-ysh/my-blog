package com.example.blog.auth;

import org.springframework.security.core.Authentication;

/** 로그인 정보에서 권한을 확인하는 도우미. */
public final class Roles {

    private Roles() {
    }

    /**
     * 관리자로 로그인한 요청인지. 로그인하지 않은 요청은 authentication이 null이다.
     * 누구나 부를 수 있는 API에서 "관리자라면 다르게 처리"할 때 쓴다.
     * (관리자만 부를 수 있는 API는 SecurityConfig의 규칙으로 막는다.)
     */
    public static boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
