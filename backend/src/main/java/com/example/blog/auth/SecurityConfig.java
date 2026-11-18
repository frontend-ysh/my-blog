package com.example.blog.auth;

import com.example.blog.image.ImageProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 보안 설정. 모든 요청은 컨트롤러에 닿기 전에 여기서 정한 필터들을 거친다.
 *
 * <pre>
 * 요청 → [토큰 검증 필터] → [권한 검사 필터] → 컨트롤러
 *          Authorization 헤더의      아래 authorizeHttpRequests
 *          JWT를 확인해 "누구인지"     규칙으로 "해도 되는지" 판단
 *          알아낸다 (인증)            (인가)
 * </pre>
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AdminProperties.class, ImageProperties.class})
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler) {
        return http
                // CSRF는 브라우저가 쿠키를 자동으로 실어 보내는 점을 노린 공격이다.
                // 이 API는 쿠키가 아니라 Authorization 헤더로 인증하므로 해당하지 않는다.
                .csrf(AbstractHttpConfigurer::disable)
                // 서버에 로그인 상태(세션)를 저장하지 않는다. 매 요청의 토큰만 본다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 위에서부터 순서대로 비교해, 처음 맞는 규칙이 적용된다.
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/*", "/api/tags").permitAll()
                        // 댓글은 누구나 읽고 쓸 수 있다. 지울 자격(비밀번호 또는 관리자)은 CommentService가 확인한다.
                        // 아래의 "/api/posts/**는 관리자만" 규칙보다 먼저 와야 한다.
                        .requestMatchers(HttpMethod.GET, "/api/posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/*").permitAll()
                        // 올린 이미지는 누구나 볼 수 있다. 올리는 것(/api/images)은 관리자만.
                        .requestMatchers(HttpMethod.GET, "/images/*").permitAll()
                        .requestMatchers("/api/images").hasRole("ADMIN")
                        .requestMatchers("/actuator/health").permitAll()
                        // 글을 쓰고 고치고 지우는 것은 관리자만.
                        .requestMatchers("/api/posts/**").hasRole("ADMIN")
                        // 규칙을 빠뜨린 주소는 기본적으로 막는다. 새 API를 만들고 규칙을
                        // 깜빡해도 공개되지 않도록 하기 위해서다.
                        .anyRequest().authenticated())
                // Authorization: Bearer 헤더의 JWT를 검증하는 필터를 켠다.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .build();
    }

    /** 토큰의 roles 클레임(["ADMIN"])을 시큐리티의 권한 이름("ROLE_ADMIN")으로 바꾼다. */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** 토큰을 만들 때 서명하는 쪽. */
    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return NimbusJwtEncoder.withSecretKey(properties.secretKey())
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * 요청에 실려 온 토큰을 검증하는 쪽. 서명이 우리 키로 된 것인지, 만료되지 않았는지,
     * 발급자가 맞는지 확인한다. 알고리즘을 HS256 하나로 고정해서, 토큰 머리말의
     * 알고리즘을 바꿔치기하는 공격("alg": "none" 등)을 막는다.
     */
    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.ISSUER));
        return decoder;
    }

    /**
     * 비밀번호 해시 도구. 기본 알고리즘은 bcrypt이고, 결과 앞에 "{bcrypt}"처럼 알고리즘
     * 이름을 붙여 저장한다. 나중에 더 강한 알고리즘으로 바꿔도 기존 비밀번호를 그대로 검증할 수 있다.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 로그인(아이디·비밀번호 확인)을 처리하는 관리자. MemberDetailsService로 계정을 찾고
     * PasswordEncoder로 비밀번호를 비교한다. 없는 아이디일 때도 비교에 걸리는 시간을 맞춰 주어,
     * 응답 시간으로 아이디 존재 여부를 알아내지 못하게 한다.
     */
    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
