package com.example.blog.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * {@link BaseTimeEntity}의 @CreatedDate, @LastModifiedDate가 동작하도록 켠다.
 * BlogApplication에 직접 붙이지 않고 따로 둔 이유: 나중에 컨트롤러만 띄우는
 * 테스트(@WebMvcTest)를 쓸 때 JPA 설정까지 끌려와 실패하는 것을 막기 위해서다.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    /**
     * 기록할 "현재 시각"을 마이크로초 단위로 자른다.
     * 자바의 시계는 나노초까지 주지만 PostgreSQL의 timestamptz는 마이크로초까지만 저장한다.
     * 그대로 두면 저장 직후 응답(나노초)과 나중에 DB에서 읽은 값(마이크로초)이 서로 달라진다.
     */
    @Bean
    DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(Instant.now().truncatedTo(ChronoUnit.MICROS));
    }
}
