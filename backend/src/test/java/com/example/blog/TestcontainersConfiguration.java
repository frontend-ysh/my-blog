package com.example.blog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 테스트 전용 설정. 테스트가 시작되면 PostgreSQL 컨테이너를 띄우고,
 * {@code @ServiceConnection}이 그 컨테이너의 접속 정보(URL, 계정)를
 * application.yml의 datasource 설정 대신 자동으로 연결해 준다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
