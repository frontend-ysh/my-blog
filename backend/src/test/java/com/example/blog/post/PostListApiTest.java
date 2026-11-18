package com.example.blog.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** 글 목록의 페이지 나누기, 태그 필터, 쿼리 수 테스트. */
class PostListApiTest extends ApiTestSupport {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Nested
    @DisplayName("페이지 나누기")
    class Paging {

        @Test
        @DisplayName("기본은 한 페이지에 10개씩, 최신 글부터다")
        void defaultsToTenPerPage() throws Exception {
            List<Long> ids = createPosts(12);
            List<Long> newestFirst = ids.reversed();

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(12))
                    .andExpect(jsonPath("$.totalPages").value(2));

            assertThat(idsOf("/api/posts")).isEqualTo(newestFirst.subList(0, 10));
        }

        @Test
        @DisplayName("다음 페이지는 앞 페이지와 겹치거나 빠지는 글 없이 이어진다")
        void pagesDoNotOverlap() throws Exception {
            List<Long> ids = createPosts(12);

            List<Long> seen = new ArrayList<>();
            seen.addAll(idsOf("/api/posts?page=1&size=5"));
            seen.addAll(idsOf("/api/posts?page=2&size=5"));
            seen.addAll(idsOf("/api/posts?page=3&size=5"));

            assertThat(seen).isEqualTo(ids.reversed());
            mockMvc.perform(get("/api/posts?page=3&size=5"))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.page").value(3))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("마지막 페이지를 넘어가면 빈 목록과 전체 개수를 돌려준다")
        void pageBeyondLastIsEmpty() throws Exception {
            createPosts(3);

            mockMvc.perform(get("/api/posts?page=99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.page").value(99))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("page는 1 이상, size는 1~50이어야 하고 어긋나면 400을 돌려준다")
        void rejectsOutOfRangeParameters() throws Exception {
            mockMvc.perform(get("/api/posts?page=0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("page"))
                    .andExpect(jsonPath("$.errors[0].message").value("page는 1 이상이어야 합니다."));
            mockMvc.perform(get("/api/posts?size=0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("size"));
            mockMvc.perform(get("/api/posts?size=51"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("size는 50 이하여야 합니다."));
            mockMvc.perform(get("/api/posts?size=50")).andExpect(status().isOk());
            mockMvc.perform(get("/api/posts?page=abc")).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("태그 필터")
    class TagFilter {

        @Test
        @DisplayName("목록의 각 항목에 태그가 실린다")
        void listItemsCarryTags() throws Exception {
            createPost("제목", "본문", "spring", "jpa");

            mockMvc.perform(get("/api/posts")).andExpect(jsonPath("$.content[0].tags", contains("jpa", "spring")));
        }

        @Test
        @DisplayName("tag를 주면 그 태그가 붙은 글만, 그 개수 기준으로 돌려준다")
        void filtersByTag() throws Exception {
            long springOnly = createPost("스프링 글", "본문", "spring");
            createPost("도커 글", "본문", "docker");
            long both = createPost("둘 다", "본문", "spring", "docker");
            createPost("태그 없는 글", "본문");

            mockMvc.perform(get("/api/posts?tag=spring"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(both))
                    // 필터에 걸린 글이라도 그 글의 태그는 전부 보여준다.
                    .andExpect(jsonPath("$.content[0].tags", contains("docker", "spring")))
                    .andExpect(jsonPath("$.content[1].id").value(springOnly));
        }

        @Test
        @DisplayName("tag의 대소문자와 공백은 구분하지 않는다")
        void tagFilterIsNormalized() throws Exception {
            createPost("제목", "본문", "spring boot");

            mockMvc.perform(get("/api/posts").param("tag", " Spring  BOOT "))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("없는 태그로 찾으면 404가 아니라 빈 목록을 돌려준다")
        void unknownTagGivesEmptyPage() throws Exception {
            createPost("제목", "본문", "spring");

            mockMvc.perform(get("/api/posts?tag=nothing"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("tag가 비어 있으면 전체 목록을 돌려준다")
        void blankTagMeansNoFilter() throws Exception {
            createPost("제목", "본문", "spring");
            createPost("제목", "본문");

            mockMvc.perform(get("/api/posts?tag=")).andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("태그 필터와 페이지를 함께 쓸 수 있다")
        void filterAndPagingTogether() throws Exception {
            List<Long> tagged = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                tagged.add(createPost("스프링 " + i, "본문", "spring"));
                createPost("기타 " + i, "본문", "etc");
            }

            mockMvc.perform(get("/api/posts?tag=spring&size=2&page=2"))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(tagged.getFirst()))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }
    }

    @Nested
    @DisplayName("검색")
    class Search {

        @Test
        @DisplayName("q를 주면 제목이나 본문에 그 말이 들어 있는 글만 돌려준다")
        void searchesTitleAndContent() throws Exception {
            long inTitle = createPost("JPA 지연 로딩", "본문");
            long inContent = createPost("다른 제목", "여기서 JPA 이야기를 한다");
            createPost("관계없는 글", "관계없는 본문");

            mockMvc.perform(get("/api/posts").param("q", "JPA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[*].id", contains((int) inContent, (int) inTitle)));
        }

        @Test
        @DisplayName("대소문자를 구분하지 않고, 한글과 낱말 중간도 찾는다")
        void isCaseInsensitiveAndMatchesSubstrings() throws Exception {
            createPost("Spring Boot 시작하기", "본문");
            createPost("스프링부트로 블로그 만들기", "본문");

            mockMvc.perform(get("/api/posts").param("q", "spring BOOT")).andExpect(jsonPath("$.totalElements").value(1));
            mockMvc.perform(get("/api/posts").param("q", "프링부")).andExpect(jsonPath("$.totalElements").value(1));
            mockMvc.perform(get("/api/posts").param("q", "  시작하기  ")).andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("검색어의 %, _, \\ 는 특수 기호가 아니라 글자 그대로 찾는다")
        void treatsLikeWildcardsLiterally() throws Exception {
            createPost("100% 달성", "본문");
            createPost("100점 달성", "본문");
            createPost("snake_case 이름", "본문");
            createPost("snakeXcase 이름", "본문");
            createPost("경로는 C:\\temp", "본문");

            // %가 "아무 글자들"로 해석되면 5개가 모두 나온다.
            mockMvc.perform(get("/api/posts").param("q", "%")).andExpect(jsonPath("$.totalElements").value(1));
            mockMvc.perform(get("/api/posts").param("q", "100%")).andExpect(jsonPath("$.totalElements").value(1));
            // _가 "아무 글자 하나"로 해석되면 snakeXcase도 나온다.
            mockMvc.perform(get("/api/posts").param("q", "snake_case")).andExpect(jsonPath("$.totalElements").value(1));
            mockMvc.perform(get("/api/posts").param("q", "_")).andExpect(jsonPath("$.totalElements").value(1));
            mockMvc.perform(get("/api/posts").param("q", "\\")).andExpect(jsonPath("$.totalElements").value(1));
            mockMvc.perform(get("/api/posts").param("q", "C:\\temp")).andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("검색어가 비어 있으면 전체 목록을 돌려준다")
        void blankQueryMeansNoFilter() throws Exception {
            createPosts(3);

            mockMvc.perform(get("/api/posts").param("q", "")).andExpect(jsonPath("$.totalElements").value(3));
            mockMvc.perform(get("/api/posts").param("q", "   ")).andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("맞는 글이 없으면 빈 목록을 돌려준다")
        void noMatchGivesEmptyPage() throws Exception {
            createPosts(3);

            mockMvc.perform(get("/api/posts").param("q", "어디에도없는말"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("검색, 태그 필터, 페이지를 함께 쓸 수 있다")
        void combinesWithTagAndPaging() throws Exception {
            List<Long> matching = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                matching.add(createPost("캐시 이야기 " + i, "본문", "spring"));
            }
            createPost("캐시 이야기 (태그 다름)", "본문", "redis");
            createPost("다른 이야기", "본문", "spring");

            mockMvc.perform(get("/api/posts").param("q", "캐시").param("tag", "spring").param("size", "2"))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.content[*].id", contains(matching.get(2).intValue(), matching.get(1).intValue())));
            mockMvc.perform(get("/api/posts").param("q", "캐시").param("tag", "spring").param("size", "2").param("page", "2"))
                    .andExpect(jsonPath("$.content[*].id", contains(matching.get(0).intValue())));
        }

        @Test
        @DisplayName("검색어는 100자까지 받고 101자부터 400을 돌려준다")
        void limitsQueryLength() throws Exception {
            mockMvc.perform(get("/api/posts").param("q", "가".repeat(100))).andExpect(status().isOk());
            mockMvc.perform(get("/api/posts").param("q", "가".repeat(101)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("q"))
                    .andExpect(jsonPath("$.errors[0].message").value("검색어는 100자 이하여야 합니다."));
        }
    }

    @Nested
    @DisplayName("쿼리 수")
    class QueryCount {

        @Test
        @DisplayName("글이 몇 개든 목록 한 번에 나가는 쿼리 수는 같다 (글마다 태그를 따로 조회하지 않는다)")
        void listDoesNotQueryTagsPerPost() throws Exception {
            for (int i = 1; i <= 12; i++) {
                createPost("글 " + i, "본문", "common", "tag" + i);
            }
            Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

            statistics.clear();
            mockMvc.perform(get("/api/posts?size=2")).andExpect(jsonPath("$.content", hasSize(2)));
            long queriesForTwo = statistics.getPrepareStatementCount();

            statistics.clear();
            mockMvc.perform(get("/api/posts?size=10")).andExpect(jsonPath("$.content", hasSize(10)));
            long queriesForTen = statistics.getPrepareStatementCount();

            // 글 조회, 전체 개수, 태그 묶음 조회(연결 테이블과 태그) 정도면 충분하다.
            assertThat(queriesForTen).isEqualTo(queriesForTwo).isLessThanOrEqualTo(4);
        }
    }

    /** 글을 n개 만들고, 만든 순서대로 id를 돌려준다. */
    private List<Long> createPosts(int n) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            ids.add(createPost("글 " + i, "본문 " + i));
        }
        assertThat(ids).isSortedAccordingTo(Comparator.naturalOrder());
        return ids;
    }

    private List<Long> idsOf(String url) throws Exception {
        List<Number> ids = JsonPath.parse(bodyOf(mockMvc.perform(get(url)))).read("$.content[*].id");
        return ids.stream().map(Number::longValue).toList();
    }
}
