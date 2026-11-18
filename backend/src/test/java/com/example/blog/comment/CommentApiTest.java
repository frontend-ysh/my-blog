package com.example.blog.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 댓글 API 테스트. 방문자는 이름·비밀번호로, 관리자는 로그인으로 댓글을 쓰고 지운다. */
class CommentApiTest extends ApiTestSupport {

    @Autowired
    CommentRepository commentRepository;

    @Nested
    @DisplayName("방문자가 댓글 쓰기")
    class GuestCreate {

        @Test
        @DisplayName("이름·비밀번호·내용으로 댓글을 쓰면 201을 돌려주고, 응답에 비밀번호는 없다")
        void createsComment() throws Exception {
            long postId = createPost("글", "본문");

            requestComment(postId, "  방문자  ", "pass1234", "잘 읽었습니다.\n둘째 줄")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.authorName").value("방문자"))
                    .andExpect(jsonPath("$.content").value("잘 읽었습니다.\n둘째 줄"))
                    .andExpect(jsonPath("$.byAdmin").value(false))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("비밀번호는 해시로 저장된다")
        void storesHashedPassword() throws Exception {
            long postId = createPost("글", "본문");
            requestComment(postId, "방문자", "pass1234", "내용").andExpect(status().isCreated());

            Comment saved = commentRepository.findAll().getFirst();
            assertThat(saved.getPassword()).startsWith("{bcrypt}").doesNotContain("pass1234");
        }

        @Test
        @DisplayName("이름과 비밀번호가 없으면 400과 필드별 오류를 돌려주고 저장하지 않는다")
        void requiresNameAndPassword() throws Exception {
            long postId = createPost("글", "본문");

            requestComment(postId, null, null, "내용")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[*].field", contains("authorName", "password")))
                    .andExpect(jsonPath("$.errors[0].message").value("이름은 필수입니다."))
                    .andExpect(jsonPath("$.errors[1].message").value("비밀번호는 필수입니다."));
            requestComment(postId, "   ", "pass1234", "내용")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", contains("authorName")));

            assertThat(commentRepository.count()).isZero();
        }

        @Test
        @DisplayName("길이 규칙: 이름 20자, 비밀번호 4~50자, 내용 1000자")
        void enforcesLengths() throws Exception {
            long postId = createPost("글", "본문");

            requestComment(postId, "가".repeat(20), "1234", "나".repeat(1000)).andExpect(status().isCreated());

            requestComment(postId, "가".repeat(21), "1234", "내용")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("이름은 20자 이하여야 합니다."));
            requestComment(postId, "이름", "123", "내용")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("비밀번호는 4자 이상 50자 이하여야 합니다."));
            requestComment(postId, "이름", "1234", "나".repeat(1001))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("내용은 1000자 이하여야 합니다."));
            requestComment(postId, "이름", "1234", "  ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("내용은 필수입니다."));

            assertThat(commentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("없는 글에는 댓글을 쓸 수 없다")
        void rejectsUnknownPost() throws Exception {
            requestComment(999, "방문자", "pass1234", "내용")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("글을 찾을 수 없습니다. id=999"));
        }

        @Test
        @DisplayName("이름을 관리자 아이디로 적어도 관리자 표시는 붙지 않는다")
        void cannotImpersonateAdminByName() throws Exception {
            long postId = createPost("글", "본문");

            requestComment(postId, "test-admin", "pass1234", "내가 관리자다")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.byAdmin").value(false));
        }
    }

    @Nested
    @DisplayName("관리자가 댓글 쓰기")
    class AdminCreate {

        @Test
        @DisplayName("관리자는 이름·비밀번호 없이 쓰고, 댓글에 관리자 표시와 로그인 아이디가 붙는다")
        void createsAdminComment() throws Exception {
            long postId = createPost("글", "본문");

            mockMvc.perform(commentRequest(postId, null, null, "답글입니다")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.authorName").value("test-admin"))
                    .andExpect(jsonPath("$.byAdmin").value(true));

            assertThat(commentRepository.findAll().getFirst().getPassword()).isNull();
        }

        @Test
        @DisplayName("관리자가 요청에 다른 이름을 적어 보내도 로그인 아이디를 쓴다")
        void ignoresNameFromAdmin() throws Exception {
            long postId = createPost("글", "본문");

            mockMvc.perform(commentRequest(postId, "다른 이름", "pass1234", "답글")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                    .andExpect(jsonPath("$.authorName").value("test-admin"));
        }

        @Test
        @DisplayName("관리자가 아닌 계정으로 로그인한 요청은 방문자와 똑같이 다룬다")
        void nonAdminAccountIsTreatedAsGuest() throws Exception {
            long postId = createPost("글", "본문");
            String userToken = tokenService.issue("someone", List.of("USER")).accessToken();

            mockMvc.perform(commentRequest(postId, null, null, "내용")
                            .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(commentRequest(postId, "이름", "pass1234", "내용")
                            .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.byAdmin").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/posts/{postId}/comments")
    class GetList {

        @Test
        @DisplayName("그 글의 댓글만, 쓴 순서대로 돌려준다 (로그인 불필요)")
        void listsCommentsOfThePostInOrder() throws Exception {
            long postId = createPost("글", "본문");
            long otherPostId = createPost("다른 글", "본문");
            createComment(postId, "첫째", "pass1234", "1");
            createComment(otherPostId, "남의 글", "pass1234", "x");
            createComment(postId, "둘째", "pass1234", "2");

            mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].authorName", contains("첫째", "둘째")))
                    .andExpect(jsonPath("$[0].password").doesNotExist());
        }

        @Test
        @DisplayName("댓글이 없으면 빈 배열, 없는 글이면 404")
        void emptyAndUnknown() throws Exception {
            long postId = createPost("글", "본문");

            mockMvc.perform(get("/api/posts/{postId}/comments", postId)).andExpect(content().json("[]"));
            mockMvc.perform(get("/api/posts/{postId}/comments", 999)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/comments/{id}")
    class Delete {

        @Test
        @DisplayName("쓸 때 정한 비밀번호가 맞으면 지운다")
        void deletesWithCorrectPassword() throws Exception {
            long postId = createPost("글", "본문");
            long target = createComment(postId, "방문자", "pass1234", "지울 댓글");
            createComment(postId, "다른 사람", "other-pass", "남을 댓글");

            requestDelete(target, "pass1234").andExpect(status().isNoContent());

            mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                    .andExpect(jsonPath("$[*].content", contains("남을 댓글")));
        }

        @Test
        @DisplayName("비밀번호가 틀리거나 없으면 403을 돌려주고 지우지 않는다")
        void rejectsWrongPassword() throws Exception {
            long postId = createPost("글", "본문");
            long id = createComment(postId, "방문자", "pass1234", "내용");

            requestDelete(id, "wrong-password")
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("비밀번호가 올바르지 않습니다."));
            requestDelete(id, null).andExpect(status().isForbidden());
            // 본문이 아예 없는 요청
            mockMvc.perform(delete("/api/comments/{id}", id)).andExpect(status().isForbidden());

            assertThat(commentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("관리자는 비밀번호 없이 어떤 댓글이든 지울 수 있다")
        void adminDeletesWithoutPassword() throws Exception {
            long postId = createPost("글", "본문");
            long id = createComment(postId, "방문자", "pass1234", "내용");

            mockMvc.perform(delete("/api/comments/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                    .andExpect(status().isNoContent());

            assertThat(commentRepository.count()).isZero();
        }

        @Test
        @DisplayName("관리자가 쓴 댓글은 방문자가 어떤 비밀번호로도 지울 수 없다")
        void guestCannotDeleteAdminComment() throws Exception {
            long postId = createPost("글", "본문");
            long id = readId(bodyOf(mockMvc.perform(commentRequest(postId, null, null, "관리자 댓글")
                    .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))));

            requestDelete(id, "").andExpect(status().isForbidden());
            requestDelete(id, "null").andExpect(status().isForbidden());
            requestDelete(id, null).andExpect(status().isForbidden());

            assertThat(commentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("없는 댓글을 지우면 404를 돌려준다")
        void returnsNotFound() throws Exception {
            requestDelete(999, "pass1234")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("댓글을 찾을 수 없습니다. id=999"));
        }

        @Test
        @DisplayName("글을 지우면 그 글의 댓글도 함께 지워진다")
        void deletingPostDeletesItsComments() throws Exception {
            long postId = createPost("지울 글", "본문");
            long otherPostId = createPost("남길 글", "본문");
            createComment(postId, "방문자", "pass1234", "함께 지워질 댓글");
            createComment(otherPostId, "방문자", "pass1234", "남을 댓글");

            mockMvc.perform(delete("/api/posts/{id}", postId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                    .andExpect(status().isNoContent());

            assertThat(commentRepository.findAll()).extracting(Comment::getContent).containsExactly("남을 댓글");
        }
    }

    private MockHttpServletRequestBuilder commentRequest(long postId, String name, String password, String text) {
        // Map.of는 null을 담지 못한다. 빠진 항목은 아예 보내지 않는다.
        Map<String, String> body = new HashMap<>();
        if (name != null) {
            body.put("authorName", name);
        }
        if (password != null) {
            body.put("password", password);
        }
        body.put("content", text);
        return post("/api/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body));
    }

    private ResultActions requestComment(long postId, String name, String password, String text) throws Exception {
        return mockMvc.perform(commentRequest(postId, name, password, text));
    }

    private long createComment(long postId, String name, String password, String text) throws Exception {
        String body = bodyOf(requestComment(postId, name, password, text).andExpect(status().isCreated()));
        return JsonPath.parse(body).read("$.id", Long.class);
    }

    private ResultActions requestDelete(long id, String password) throws Exception {
        Map<String, String> body = new HashMap<>();
        if (password != null) {
            body.put("password", password);
        }
        return mockMvc.perform(delete("/api/comments/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body)));
    }
}
