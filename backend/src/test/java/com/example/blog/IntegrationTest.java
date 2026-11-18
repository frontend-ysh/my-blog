package com.example.blog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 통합 테스트용 묶음 애너테이션. 애플리케이션 전체를 띄우고(@SpringBootTest),
 * 가짜 HTTP 요청을 보낼 MockMvc를 준비하고, 일회용 PostgreSQL에 연결하고,
 * 테스트용 설정(application-test.yml)을 켠다.
 *
 * <p>스프링은 설정이 같은 테스트 클래스끼리 애플리케이션 컨텍스트를 재사용한다.
 * 모든 통합 테스트가 이 애너테이션 하나만 쓰면 설정이 같아지므로,
 * 테스트 클래스가 늘어나도 앱과 DB 컨테이너는 한 번만 뜬다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public @interface IntegrationTest {
}
