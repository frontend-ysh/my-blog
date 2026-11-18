package com.example.blog.tag;

import com.example.blog.tag.dto.TagResponse;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagResponse> getList() {
        return tagRepository.findAllWithPostCount();
    }

    /**
     * 태그 이름들을 Tag 엔티티로 바꾼다. 이미 있는 태그는 찾아서 재사용하고, 없는 것만 새로 만든다.
     * 글을 저장하는 트랜잭션 안에서 불리므로, 글 저장이 실패하면 새로 만든 태그도 함께 취소된다.
     */
    @Transactional
    public Set<Tag> resolve(Collection<String> rawNames) {
        // 표기를 통일하고 중복을 없앤다. ("Spring", "spring" → "spring" 하나)
        Set<String> names = rawNames.stream()
                .map(Tag::normalize)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (names.isEmpty()) {
            return Set.of();
        }

        // 태그마다 하나씩 조회하지 않고, 한 번의 쿼리(where name in (...))로 모두 찾는다.
        Map<String, Tag> existing = tagRepository.findByNameIn(names).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        Set<Tag> tags = new LinkedHashSet<>();
        for (String name : names) {
            Tag tag = existing.get(name);
            tags.add(tag != null ? tag : tagRepository.save(new Tag(name)));
        }
        return tags;
    }
}
