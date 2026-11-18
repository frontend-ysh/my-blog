package com.example.blog.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml의 blog.jwt.* 설정을 담는다.
 * 값이 규칙에 맞지 않으면 서버가 뜨지 않는다 (약한 키로 운영되는 것을 막는다).
 *
 * @param secret     토큰 서명에 쓰는 비밀 키. 이 값을 아는 사람은 관리자 토큰을 만들 수 있다.
 * @param expiration 토큰 유효 기간 (예: 12h, 30m)
 */
@Validated
@ConfigurationProperties("blog.jwt")
public record JwtProperties(
        // HS256 서명은 256비트(32바이트) 이상의 키가 필요하다.
        @NotBlank(message = "비밀 키가 비어 있습니다. JWT_SECRET 환경변수를 설정하세요.")
        @Size(min = 32, message = "비밀 키는 32자 이상이어야 합니다.")
        String secret,

        @NotNull
        Duration expiration
) {

    public SecretKey secretKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
