package com.example.blog.comment;

import com.example.blog.comment.dto.CommentRequest;
import com.example.blog.comment.dto.CommentResponse;
import com.example.blog.common.FieldViolation;
import com.example.blog.common.ForbiddenException;
import com.example.blog.common.InvalidInputException;
import com.example.blog.post.Post;
import com.example.blog.post.PostNotFoundException;
import com.example.blog.post.PostRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    public CommentService(
            CommentRepository commentRepository,
            PostRepository postRepository,
            PasswordEncoder passwordEncoder) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<CommentResponse> getList(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }
        return commentRepository.findByPostIdOrderByCreatedAtAscIdAsc(postId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    /**
     * @param adminName 관리자로 로그인한 요청이면 그 아이디, 방문자의 요청이면 null
     */
    @Transactional
    public CommentResponse create(Long postId, CommentRequest request, String adminName) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));

        Comment comment;
        if (adminName != null) {
            // 관리자는 요청에 이름을 적어 보내도 무시하고, 로그인한 아이디를 쓴다.
            comment = Comment.byAdmin(post, adminName, request.content());
        } else {
            requireGuestFields(request);
            // 댓글 비밀번호도 로그인 비밀번호와 똑같이 해시로 저장한다.
            String encodedPassword = passwordEncoder.encode(request.password());
            comment = Comment.byGuest(post, request.authorName().strip(), encodedPassword, request.content());
        }
        return CommentResponse.from(commentRepository.save(comment));
    }

    /**
     * @param admin 관리자로 로그인한 요청인지. 관리자는 비밀번호 없이 어떤 댓글이든 지울 수 있다.
     */
    @Transactional
    public void delete(Long id, String password, boolean admin) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentNotFoundException(id));

        if (!admin && !matchesPassword(comment, password)) {
            throw new ForbiddenException("비밀번호가 올바르지 않습니다.");
        }
        commentRepository.delete(comment);
    }

    private boolean matchesPassword(Comment comment, String password) {
        // 관리자가 쓴 댓글은 비밀번호가 없다. 어떤 비밀번호를 보내도 맞지 않는다.
        return comment.getPassword() != null
                && password != null
                && passwordEncoder.matches(password, comment.getPassword());
    }

    private static void requireGuestFields(CommentRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        if (!StringUtils.hasText(request.authorName())) {
            violations.add(new FieldViolation("authorName", "이름은 필수입니다."));
        }
        if (!StringUtils.hasText(request.password())) {
            violations.add(new FieldViolation("password", "비밀번호는 필수입니다."));
        }
        if (!violations.isEmpty()) {
            throw new InvalidInputException(violations);
        }
    }
}
