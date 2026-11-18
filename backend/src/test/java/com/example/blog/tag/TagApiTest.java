package com.example.blog.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.ApiTestSupport;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.json.JsonCompareMode;

/** 글에 태그를 붙이고 떼는 동작과, 태그 목록 API 테스트. */
class TagApiTest extends ApiTestSupport {

    @Nested
    @DisplayName("글에 태그 붙이기")
    class TagsOnPost {

        @Test
        @DisplayName("태그와 함께 글을 쓰면 이름순으로 정렬된 태그가 응답에 실리고, 다시 조회해도 같다")
        void createsPostWithTags() throws Exception {
            String created = bodyOf(requestCreate("제목", "본문", "spring", "jpa", "docker")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tags", contains("docker", "jpa", "spring"))));

            mockMvc.perform(get("/api/posts/{id}", readId(created)))
                    .andExpect(content().json(created, JsonCompareMode.STRICT));
        }

        @Test
        @DisplayName("태그 없이 글을 쓰면 빈 배열이 실린다")
        void tagsAreOptional() throws Exception {
            requestCreate("제목", "본문").andExpect(jsonPath("$.tags", hasSize(0)));

            // tags 항목이 아예 없는 요청도 받아들인다.
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/posts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"제목\",\"content\":\"본문\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tags", hasSize(0)));
        }

        @Test
        @DisplayName("대소문자와 공백이 달라도 같은 태그로 합쳐진다")
        void normalizesNames() throws Exception {
            requestCreate("제목", "본문", " Spring ", "SPRING", "Spring   Boot", "스프링")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tags", contains("spring", "spring boot", "스프링")));

            assertThat(tagRepository.count()).isEqualTo(3);
        }

        @Test
        @DisplayName("이미 있는 태그는 새로 만들지 않고 다시 쓴다")
        void reusesExistingTags() throws Exception {
            createPost("첫 글", "본문", "spring", "jpa");
            createPost("둘째 글", "본문", "Spring", "docker");

            assertThat(tagRepository.count()).isEqualTo(3);
        }

        @Test
        @DisplayName("수정하면 빠진 태그는 떼고 새 태그는 붙인다")
        void replacesTagsOnUpdate() throws Exception {
            long id = createPost("제목", "본문", "spring", "jpa");

            String updated = bodyOf(requestUpdate(id, "제목", "본문", "jpa", "docker")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tags", contains("docker", "jpa"))));

            mockMvc.perform(get("/api/posts/{id}", id))
                    .andExpect(content().json(updated, JsonCompareMode.STRICT));
        }

        @Test
        @DisplayName("수정에서 태그를 모두 빼면 태그 없는 글이 된다")
        void removesAllTagsOnUpdate() throws Exception {
            long id = createPost("제목", "본문", "spring", "jpa");

            requestUpdate(id, "제목", "본문").andExpect(jsonPath("$.tags", hasSize(0)));

            mockMvc.perform(get("/api/posts/{id}", id)).andExpect(jsonPath("$.tags", hasSize(0)));
        }

        @Test
        @DisplayName("글을 지워도 다른 글에 붙은 같은 태그는 그대로다")
        void deletingPostKeepsTagsOfOtherPosts() throws Exception {
            long first = createPost("지울 글", "본문", "spring", "jpa");
            long second = createPost("남길 글", "본문", "spring");

            mockMvc.perform(delete("/api/posts/{id}", first).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/posts/{id}", second)).andExpect(jsonPath("$.tags", contains("spring")));
        }
    }

    @Nested
    @DisplayName("태그 검증")
    class Validation {

        @Test
        @DisplayName("태그는 10개까지 붙일 수 있고 11개부터 거절한다")
        void limitsTagCount() throws Exception {
            String[] ten = IntStream.rangeClosed(1, 10).mapToObj(i -> "tag" + i).toArray(String[]::new);
            String[] eleven = IntStream.rangeClosed(1, 11).mapToObj(i -> "tag" + i).toArray(String[]::new);

            requestCreate("제목", "본문", ten).andExpect(status().isCreated());
            requestCreate("제목", "본문", eleven)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("tags"))
                    .andExpect(jsonPath("$.errors[0].message").value("태그는 10개까지 붙일 수 있습니다."));

            assertThat(postRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("태그 이름은 30자까지 허용하고 31자부터 거절한다")
        void limitsTagLength() throws Exception {
            requestCreate("제목", "본문", "가".repeat(30)).andExpect(status().isCreated());
            requestCreate("제목", "본문", "ok", "가".repeat(31))
                    .andExpect(status().isBadRequest())
                    // 몇 번째 태그가 틀렸는지 알려준다.
                    .andExpect(jsonPath("$.errors[0].field").value("tags[1]"))
                    .andExpect(jsonPath("$.errors[0].message").value("태그는 30자 이하여야 합니다."));
        }

        @Test
        @DisplayName("빈 태그와 쉼표가 든 태그는 거절하고, 글도 태그도 저장하지 않는다")
        void rejectsBlankAndComma() throws Exception {
            requestCreate("제목", "본문", "  ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("빈 태그는 넣을 수 없습니다."));
            requestCreate("제목", "본문", "new-tag", "a,b")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("태그에는 쉼표를 쓸 수 없습니다."));

            assertThat(postRepository.count()).isZero();
            assertThat(tagRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("GET /api/tags")
    class TagList {

        @Test
        @DisplayName("글이 많이 붙은 태그부터, 같으면 이름순으로 돌려준다 (로그인 불필요)")
        void listsTagsWithPostCount() throws Exception {
            createPost("글 1", "본문", "spring", "jpa");
            createPost("글 2", "본문", "spring");
            createPost("글 3", "본문", "docker");

            mockMvc.perform(get("/api/tags"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("""
                            [
                              {"name": "spring", "postCount": 2},
                              {"name": "docker", "postCount": 1},
                              {"name": "jpa", "postCount": 1}
                            ]
                            """, JsonCompareMode.STRICT));
        }

        @Test
        @DisplayName("어느 글에도 붙어 있지 않은 태그는 나오지 않는다")
        void hidesUnusedTags() throws Exception {
            long id = createPost("글", "본문", "spring", "jpa");
            requestUpdate(id, "글", "본문", "spring").andExpect(status().isOk());

            mockMvc.perform(get("/api/tags"))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("spring"));
        }

        @Test
        @DisplayName("글이 없으면 빈 배열을 돌려준다")
        void returnsEmptyArray() throws Exception {
            mockMvc.perform(get("/api/tags")).andExpect(status().isOk()).andExpect(content().json("[]"));
        }
    }
}
