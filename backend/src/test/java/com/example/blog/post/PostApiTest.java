package com.example.blog.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.ApiTestSupport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.json.JsonCompareMode;

/** 글 작성·조회·수정·삭제와 권한 테스트. 목록의 페이지·태그 필터는 PostListApiTest에 있다. */
class PostApiTest extends ApiTestSupport {

    @Nested
    @DisplayName("POST /api/posts")
    class Create {

        @Test
        @DisplayName("글을 작성하면 201과 Location을 돌려주고, 그 주소로 같은 글이 조회된다")
        void createsPost() throws Exception {
            MockHttpServletResponse response = requestCreate("첫 글", "본문입니다.")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("첫 글"))
                    .andExpect(jsonPath("$.content").value("본문입니다."))
                    .andReturn().getResponse();

            String created = response.getContentAsString(StandardCharsets.UTF_8);
            String location = response.getHeader("Location");
            assertThat(location).isEqualTo("/api/posts/" + readId(created));

            // 작성 직후 응답과 나중에 조회한 응답이 (시각까지) 완전히 같아야 한다.
            mockMvc.perform(get(location))
                    .andExpect(status().isOk())
                    .andExpect(content().json(created, JsonCompareMode.STRICT));
        }

        @Test
        @DisplayName("새 글은 생성 시각과 수정 시각이 같다")
        void newPostHasSameTimestamps() throws Exception {
            String created = bodyOf(requestCreate("제목", "본문"));

            assertThat(readInstant(created, "$.updatedAt")).isEqualTo(readInstant(created, "$.createdAt"));
        }

        @Test
        @DisplayName("제목이 공백뿐이면 400과 필드별 오류를 돌려주고 저장하지 않는다")
        void rejectsBlankTitle() throws Exception {
            requestCreate("   ", "본문")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("title"))
                    .andExpect(jsonPath("$.errors[0].message").value("제목은 필수입니다."));

            assertThat(postRepository.count()).isZero();
        }

        @Test
        @DisplayName("제목은 200자까지 허용하고 201자부터 거절한다")
        void enforcesTitleLength() throws Exception {
            requestCreate("가".repeat(200), "본문").andExpect(status().isCreated());

            requestCreate("가".repeat(201), "본문")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("title"))
                    .andExpect(jsonPath("$.errors[0].message").value("제목은 200자 이하여야 합니다."));

            assertThat(postRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("필드가 여러 개 틀리면 오류를 필드 이름 순으로 모두 알려준다")
        void reportsAllFieldErrors() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[0].field").value("content"))
                    .andExpect(jsonPath("$.errors[1].field").value("title"));
        }

        @Test
        @DisplayName("JSON 형식이 깨져 있으면 400을 돌려준다")
        void rejectsMalformedJson() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Nested
    @DisplayName("GET /api/posts")
    class GetList {

        @Test
        @DisplayName("글이 없으면 빈 목록을 돌려준다")
        void returnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("최신 글부터 돌려주고, 목록에는 본문을 싣지 않는다")
        void returnsNewestFirstWithoutContent() throws Exception {
            long first = createPost("첫째", "본문 1");
            long second = createPost("둘째", "본문 2");
            long third = createPost("셋째", "본문 3");

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.content[0].id").value(third))
                    .andExpect(jsonPath("$.content[0].title").value("셋째"))
                    .andExpect(jsonPath("$.content[0].createdAt").exists())
                    .andExpect(jsonPath("$.content[0].content").doesNotExist())
                    .andExpect(jsonPath("$.content[1].id").value(second))
                    .andExpect(jsonPath("$.content[2].id").value(first));
        }
    }

    @Nested
    @DisplayName("GET /api/posts/{id}")
    class GetOne {

        @Test
        @DisplayName("없는 글을 조회하면 404를 돌려준다")
        void returnsNotFound() throws Exception {
            mockMvc.perform(get("/api/posts/{id}", 999))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("글을 찾을 수 없습니다. id=999"));
        }

        @Test
        @DisplayName("id가 숫자가 아니면 400을 돌려준다")
        void rejectsNonNumericId() throws Exception {
            mockMvc.perform(get("/api/posts/abc")).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/posts/{id}")
    class Update {

        @Test
        @DisplayName("글을 수정하면 바뀐 내용과 새 수정 시각을 돌려주고, 생성 시각은 그대로다")
        void updatesPost() throws Exception {
            String created = bodyOf(requestCreate("원래 제목", "원래 본문"));
            long id = readId(created);
            Instant createdAt = readInstant(created, "$.createdAt");

            String updated = bodyOf(requestUpdate(id, "바뀐 제목", "바뀐 본문")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.title").value("바뀐 제목"))
                    .andExpect(jsonPath("$.content").value("바뀐 본문")));

            assertThat(readInstant(updated, "$.createdAt")).isEqualTo(createdAt);
            assertThat(readInstant(updated, "$.updatedAt")).isAfter(createdAt);

            // 수정 응답과 이후 조회 응답이 완전히 같아야 한다.
            mockMvc.perform(get("/api/posts/{id}", id))
                    .andExpect(content().json(updated, JsonCompareMode.STRICT));
        }

        @Test
        @DisplayName("없는 글을 수정하면 404를 돌려준다")
        void returnsNotFound() throws Exception {
            requestUpdate(999, "제목", "본문").andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("입력이 틀리면 400을 돌려주고 기존 글을 바꾸지 않는다")
        void rejectsInvalidInput() throws Exception {
            long id = createPost("원래 제목", "원래 본문");

            requestUpdate(id, "바뀐 제목", "")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("content"));

            mockMvc.perform(get("/api/posts/{id}", id))
                    .andExpect(jsonPath("$.title").value("원래 제목"))
                    .andExpect(jsonPath("$.content").value("원래 본문"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/posts/{id}")
    class Delete {

        @Test
        @DisplayName("글을 삭제하면 204를 돌려주고, 그 글만 사라진다")
        void deletesPost() throws Exception {
            long target = createPost("지울 글", "본문");
            long other = createPost("남길 글", "본문");

            mockMvc.perform(delete("/api/posts/{id}", target).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            mockMvc.perform(get("/api/posts/{id}", target)).andExpect(status().isNotFound());
            mockMvc.perform(get("/api/posts/{id}", other)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("없는 글을 삭제하면 404를 돌려준다")
        void returnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/posts/{id}", 999).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("권한")
    class Authorization {

        @Test
        @DisplayName("글 목록과 상세는 로그인하지 않아도 볼 수 있다")
        void readsArePublic() throws Exception {
            long id = createPost("공개 글", "본문");

            mockMvc.perform(get("/api/posts")).andExpect(status().isOk());
            mockMvc.perform(get("/api/posts/{id}", id)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("토큰 없이 글을 쓰면 401을 돌려주고 저장하지 않는다")
        void createRequiresLogin() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson("제목", "본문")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("로그인이 필요합니다."));

            assertThat(postRepository.count()).isZero();
        }

        @Test
        @DisplayName("토큰 없이 수정·삭제하면 401을 돌려주고 글은 그대로다")
        void updateAndDeleteRequireLogin() throws Exception {
            long id = createPost("원래 제목", "원래 본문");

            mockMvc.perform(put("/api/posts/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson("바뀐 제목", "바뀐 본문")))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(delete("/api/posts/{id}", id)).andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/posts/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("원래 제목"));
        }

        @Test
        @DisplayName("관리자가 아닌 계정의 토큰으로 글을 쓰면 403을 돌려주고 저장하지 않는다")
        void writeRequiresAdminRole() throws Exception {
            String userToken = tokenService.issue("someone", List.of("USER")).accessToken();

            mockMvc.perform(post("/api/posts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson("제목", "본문")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("권한이 없습니다."));

            assertThat(postRepository.count()).isZero();
        }
    }
}
