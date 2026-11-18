package com.example.blog.post;

import com.example.blog.common.PageResponse;
import com.example.blog.post.dto.PostRequest;
import com.example.blog.post.dto.PostResponse;
import com.example.blog.post.dto.PostSummaryResponse;
import com.example.blog.tag.Tag;
import com.example.blog.tag.TagService;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 비즈니스 로직 계층. 트랜잭션의 경계가 여기다: 메서드가 정상 종료하면 커밋,
 * 예외가 나면 롤백된다. 기본은 읽기 전용이고, 쓰는 메서드만 @Transactional로 덮어쓴다.
 */
@Service
@Transactional(readOnly = true)
public class PostService {

    /** 최신 글부터. 생성 시각이 같으면 id가 큰 것부터 보여 주어 순서가 흔들리지 않게 한다. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PostRepository postRepository;
    private final TagService tagService;

    public PostService(PostRepository postRepository, TagService tagService) {
        this.postRepository = postRepository;
        this.tagService = tagService;
    }

    @Transactional
    public PostResponse create(PostRequest request) {
        Post post = new Post(request.title(), request.content(), tagService.resolve(request.tags()));
        return PostResponse.from(postRepository.save(post));
    }

    /**
     * @param page 1부터 시작하는 페이지 번호
     * @param tag  이 태그가 붙은 글만 본다. 비어 있으면 조건 없음.
     * @param q    제목이나 본문에 이 말이 들어 있는 글만 본다(대소문자 무시). 비어 있으면 조건 없음.
     */
    public PageResponse<PostSummaryResponse> getList(int page, int size, String tag, String q) {
        Pageable pageable = PageRequest.of(page - 1, size, NEWEST_FIRST);
        String tagName = StringUtils.hasText(tag) ? Tag.normalize(tag) : null;
        String pattern = StringUtils.hasText(q) ? toLikePattern(q) : null;
        Page<Post> posts = postRepository.search(tagName, pattern, pageable);

        // DTO로 바꾸면서 각 글의 태그를 읽는다. 글마다 태그 조회 쿼리가 한 번씩 나가면
        // 글이 10개일 때 쿼리가 10번 더 나간다(N+1 문제). application.yml의
        // default_batch_fetch_size 설정 덕분에 한 번의 쿼리로 묶여서 나간다.
        return PageResponse.from(posts.map(PostSummaryResponse::from));
    }

    public PostResponse get(Long id) {
        return PostResponse.from(findPost(id));
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request) {
        Post post = findPost(id);
        // save()를 부르지 않아도 된다. 트랜잭션 안에서 조회한 엔티티는 JPA가 지켜보다가
        // 값이 바뀌어 있으면 커밋할 때 UPDATE를 보낸다(변경 감지, dirty checking).
        post.update(request.title(), request.content(), tagService.resolve(request.tags()));
        // 수정 시각(updatedAt)은 UPDATE가 실제로 나가는 순간에 기록된다. 응답을 만들기 전에
        // 미리 내보내지(flush) 않으면 응답에는 수정 전 시각이 담긴다.
        postRepository.flush();
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long id) {
        postRepository.delete(findPost(id));
    }

    /**
     * 검색어를 "이 말이 어디든 들어 있으면"이라는 LIKE 패턴으로 바꾼다.
     * LIKE에서 %는 "아무 글자들", _는 "아무 글자 하나"라는 뜻이다. 검색어에 든 이 글자들을
     * 그대로 두면 "100%"를 찾으려는 검색이 "100으로 시작하는 모든 것"을 찾게 된다.
     * 앞에 \를 붙여 글자 그대로 찾게 한다.
     */
    private static String toLikePattern(String q) {
        String escaped = q.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private Post findPost(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
    }
}
