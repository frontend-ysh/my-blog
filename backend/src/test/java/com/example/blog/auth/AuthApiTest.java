package com.example.blog.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.IntegrationTest;
import com.example.blog.member.Member;
import com.example.blog.member.MemberRepository;
import com.example.blog.member.Role;
import com.example.blog.post.PostRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/** 로그인과 토큰 검증 통합 테스트. 계정 정보는 application-test.yml에 있다. */
@IntegrationTest
class AuthApiTest {

    static final String USERNAME = "test-admin";
    static final String PASSWORD = "test-password-1234";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    TokenService tokenService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        postRepository.deleteAll();
    }

    @Nested
    @DisplayName("관리자 계정")
    class AdminAccount {

        @Test
        @DisplayName("서버가 뜰 때 설정의 관리자 계정이 만들어지고, 비밀번호는 해시로 저장된다")
        void adminIsCreatedWithHashedPassword() {
            Member admin = memberRepository.findByUsername(USERNAME).orElseThrow();

            assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
            assertThat(admin.getPassword()).startsWith("{bcrypt}").doesNotContain(PASSWORD);
            assertThat(passwordEncoder.matches(PASSWORD, admin.getPassword())).isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("아이디와 비밀번호가 맞으면 토큰을 발급한다")
        void issuesToken() throws Exception {
            requestLogin(USERNAME, PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(not(emptyString())))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600));
        }

        @Test
        @DisplayName("발급받은 토큰으로 글을 쓸 수 있다")
        void issuedTokenAllowsWriting() throws Exception {
            String token = JsonPath.parse(bodyOf(requestLogin(USERNAME, PASSWORD))).read("$.accessToken");

            requestCreatePost(token).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401을 돌려준다")
        void rejectsWrongPassword() throws Exception {
            requestLogin(USERNAME, "wrong-password")
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("아이디 또는 비밀번호가 올바르지 않습니다."))
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("없는 아이디도 비밀번호가 틀렸을 때와 똑같이 응답한다 (아이디 존재 여부를 알려주지 않는다)")
        void unknownUserLooksLikeWrongPassword() throws Exception {
            String wrongPassword = bodyOf(requestLogin(USERNAME, "wrong-password").andExpect(status().isUnauthorized()));
            String unknownUser = bodyOf(requestLogin("nobody", PASSWORD).andExpect(status().isUnauthorized()));

            assertThat(unknownUser).isEqualTo(wrongPassword);
        }

        @Test
        @DisplayName("아이디나 비밀번호를 비우면 400과 필드별 오류를 돌려준다")
        void rejectsBlankFields() throws Exception {
            requestLogin(" ", "")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[0].field").value("password"))
                    .andExpect(jsonPath("$.errors[1].field").value("username"));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class Me {

        @Test
        @DisplayName("토큰의 주인과 권한을 알려준다")
        void returnsCurrentUser() throws Exception {
            String token = JsonPath.parse(bodyOf(requestLogin(USERNAME, PASSWORD))).read("$.accessToken");

            mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(USERNAME))
                    .andExpect(jsonPath("$.roles", hasSize(1)))
                    .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
        }

        @Test
        @DisplayName("토큰이 없으면 401을 돌려준다")
        void requiresToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("로그인이 필요합니다."));
        }
    }

    /** 아래 토큰들은 모두 "관리자"라고 주장하지만 받아들여서는 안 된다. */
    @Nested
    @DisplayName("잘못된 토큰으로 글쓰기")
    class InvalidTokens {

        @Test
        @DisplayName("만료된 토큰은 거절한다")
        void rejectsExpiredToken() throws Exception {
            Instant now = Instant.now();
            String token = encode(jwtEncoder, adminClaims()
                    .issuedAt(now.minusSeconds(7200))
                    .expiresAt(now.minusSeconds(3600)));

            expectRejected(token);
        }

        @Test
        @DisplayName("다른 키로 서명한 토큰은 거절한다")
        void rejectsTokenSignedWithAnotherKey() throws Exception {
            JwtEncoder attackerEncoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(
                            "attacker-does-not-know-the-real-secret-key".getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"))
                    .algorithm(MacAlgorithm.HS256)
                    .build();

            expectRejected(encode(attackerEncoder, adminClaims()));
        }

        @Test
        @DisplayName("내용을 고친 토큰은 거절한다 (일반 계정 토큰의 권한을 ADMIN으로 바꿔치기)")
        void rejectsTamperedToken() throws Exception {
            String[] parts = tokenService.issue("someone", List.of("USER")).accessToken().split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            assertThat(payload).contains("\"USER\"");
            String forgedPayload = base64Url(payload.replace("\"USER\"", "\"ADMIN\""));

            // 머리말과 서명은 그대로 두고 내용만 바꾼다. 서명이 내용과 맞지 않게 된다.
            expectRejected(parts[0] + "." + forgedPayload + "." + parts[2]);
        }

        @Test
        @DisplayName("서명이 없는 토큰(alg: none)은 거절한다")
        void rejectsUnsignedToken() throws Exception {
            String header = base64Url("{\"alg\":\"none\"}");
            String payload = base64Url(jsonMapper.writeValueAsString(Map.of(
                    "iss", TokenService.ISSUER,
                    "sub", USERNAME,
                    "roles", List.of("ADMIN"),
                    "exp", Instant.now().plusSeconds(3600).getEpochSecond())));

            expectRejected(header + "." + payload + ".");
        }

        @Test
        @DisplayName("발급자가 다른 토큰은 거절한다")
        void rejectsTokenFromAnotherIssuer() throws Exception {
            expectRejected(encode(jwtEncoder, adminClaims().issuer("someone-else")));
        }

        @Test
        @DisplayName("토큰 형식이 아닌 문자열은 거절한다")
        void rejectsGarbage() throws Exception {
            expectRejected("this-is-not-a-jwt");
        }

        private void expectRejected(String token) throws Exception {
            requestCreatePost(token)
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("로그인이 필요합니다."));

            assertThat(postRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("기본 규칙")
    class Defaults {

        @Test
        @DisplayName("규칙을 따로 정하지 않은 주소는 로그인해야 접근할 수 있다")
        void unknownPathsRequireLogin() throws Exception {
            mockMvc.perform(get("/api/anything-else")).andExpect(status().isUnauthorized());

            String token = tokenService.issue(USERNAME, List.of("ADMIN")).accessToken();
            mockMvc.perform(get("/api/anything-else").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("헬스 체크는 로그인 없이 볼 수 있다")
        void healthIsPublic() throws Exception {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        }
    }

    /** 유효 기간이 남아 있고 발급자도 맞는 관리자 클레임. 각 테스트가 한 가지만 어긋나게 바꾼다. */
    private static JwtClaimsSet.Builder adminClaims() {
        Instant now = Instant.now();
        return JwtClaimsSet.builder()
                .issuer(TokenService.ISSUER)
                .subject(USERNAME)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim(TokenService.ROLES_CLAIM, List.of("ADMIN"));
    }

    private static String encode(JwtEncoder encoder, JwtClaimsSet.Builder claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private static String base64Url(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions requestLogin(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("username", username, "password", password))));
    }

    private ResultActions requestCreatePost(String token) throws Exception {
        return mockMvc.perform(post("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("title", "제목", "content", "본문"))));
    }

    private static String bodyOf(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
