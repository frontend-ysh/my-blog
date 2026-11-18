package com.example.blog.auth;

import com.example.blog.auth.dto.TokenResponse;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * 액세스 토큰(JWT)을 발급한다.
 *
 * <p>JWT는 "누구인지(sub), 권한(roles), 언제까지 유효한지(exp)"를 적고 서버의 비밀 키로
 * 서명한 문자열이다. 서버는 토큰을 어디에도 저장하지 않는다. 요청에 실려 온 토큰의 서명과
 * 만료 시각만 확인하면 되므로 요청마다 DB를 조회할 필요가 없다.
 *
 * <p>그 대가로, 한 번 발급한 토큰은 만료 전에 서버에서 무효로 만들 수 없다.
 * 그래서 유효 기간을 너무 길게 잡지 않는다.
 */
@Service
public class TokenService {

    /** 토큰 발급자(iss). 검증할 때 이 값이 아니면 거절한다. */
    public static final String ISSUER = "blog";

    /** 권한 목록을 담는 클레임 이름. */
    public static final String ROLES_CLAIM = "roles";

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /** 로그인에 성공한 사용자의 토큰을 발급한다. */
    public TokenResponse issue(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .toList();
        return issue(authentication.getName(), roles);
    }

    public TokenResponse issue(String username, Collection<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .claim(ROLES_CLAIM, List.copyOf(roles))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", properties.expiration().toSeconds());
    }
}
