package com.example.blog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.auth.TokenService;
import com.example.blog.post.PostRepository;
import com.example.blog.tag.TagRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/**
 * API 통합 테스트들이 함께 쓰는 준비 코드. HTTP 요청부터 실제 PostgreSQL까지 전 구간을 거친다.
 *
 * <p>테스트 메서드에 @Transactional을 붙여 롤백하는 방식은 쓰지 않는다. 그러면 커밋이
 * 일어나지 않아, 커밋 시점에 동작하는 것(변경 감지, 수정 시각 기록)을 검증하지 못한다.
 * 대신 매 테스트 전에 테이블을 비운다.
 */
@IntegrationTest
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper jsonMapper;

    @Autowired
    protected PostRepository postRepository;

    @Autowired
    protected TagRepository tagRepository;

    @Autowired
    protected TokenService tokenService;

    /** 관리자 토큰. 글을 쓰고 고치고 지우는 요청에 실어 보낸다. */
    protected String adminToken;

    @BeforeEach
    void resetData() {
        postRepository.deleteAll();
        tagRepository.deleteAll();
        adminToken = tokenService.issue("test-admin", List.of("ADMIN")).accessToken();
    }

    protected ResultActions requestCreate(String title, String content, String... tags) throws Exception {
        return mockMvc.perform(post("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(title, content, tags)));
    }

    protected ResultActions requestUpdate(long id, String title, String content, String... tags) throws Exception {
        return mockMvc.perform(put("/api/posts/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(title, content, tags)));
    }

    /** 글을 하나 만들고 그 id를 돌려준다. 다른 동작을 검증하기 위한 준비용. */
    protected long createPost(String title, String content, String... tags) throws Exception {
        return readId(bodyOf(requestCreate(title, content, tags).andExpect(status().isCreated())));
    }

    /** 응답 본문을 문자열로 꺼낸다. 한글이 깨지지 않도록 UTF-8로 읽는다. */
    protected static String bodyOf(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected static long readId(String json) {
        return JsonPath.parse(json).read("$.id", Long.class);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected String toJson(String title, String content, String... tags) {
        return jsonMapper.writeValueAsString(Map.of("title", title, "content", content, "tags", List.of(tags)));
    }

    protected static Instant readInstant(String json, String path) {
        return Instant.parse(JsonPath.parse(json).read(path, String.class));
    }
}
