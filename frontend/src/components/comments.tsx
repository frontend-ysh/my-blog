import { createComment, deleteComment } from "@/app/posts/comment-actions";
import { type Comment, getComments } from "@/lib/comments";
import { formatDateTime } from "@/lib/format";
import { getAdmin } from "@/lib/session";
import { CommentDelete } from "./comment-delete";
import { CommentForm } from "./comment-form";

/** 글 아래의 댓글 영역: 목록과 작성 폼. */
export async function Comments({ postId }: { postId: number }) {
  const admin = await getAdmin();

  // 댓글을 못 불러와도 글은 읽을 수 있어야 한다. 예외를 밖으로 던지면 글 화면 전체가
  // 오류 화면으로 바뀌므로, 여기서 받아 댓글 자리에만 안내를 보여준다.
  let comments: Comment[];
  try {
    comments = await getComments(postId);
  } catch {
    return (
      <section className="mt-12 border-t border-zinc-200 pt-8 dark:border-zinc-800">
        <h2 className="text-lg font-semibold">댓글</h2>
        <p className="mt-4 text-sm text-zinc-500">
          댓글을 불러오지 못했습니다. 잠시 후 새로고침해 주세요.
        </p>
      </section>
    );
  }

  return (
    <section
      aria-labelledby="comments-heading"
      className="mt-12 border-t border-zinc-200 pt-8 dark:border-zinc-800"
    >
      <h2 id="comments-heading" className="text-lg font-semibold">
        댓글 <span className="text-zinc-500">{comments.length}</span>
      </h2>

      {comments.length > 0 && (
        <ul className="mt-4 divide-y divide-zinc-200 dark:divide-zinc-800">
          {comments.map((comment) => (
            <li key={comment.id} className="py-4">
              <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                <span className="text-sm font-semibold wrap-anywhere">
                  {comment.authorName}
                </span>
                {comment.byAdmin && (
                  <span className="rounded bg-foreground px-1.5 py-0.5 text-[11px] font-medium text-background">
                    글쓴이
                  </span>
                )}
                <time
                  dateTime={comment.createdAt}
                  className="text-xs text-zinc-500"
                >
                  {formatDateTime(comment.createdAt)}
                </time>
                {/* 글쓴이의 댓글에는 비밀번호가 없으므로 방문자에게는 삭제를 보여주지 않는다. */}
                {(admin || !comment.byAdmin) && (
                  <div className="ml-auto">
                    <CommentDelete
                      action={deleteComment.bind(null, postId, comment.id)}
                      isAdmin={Boolean(admin)}
                    />
                  </div>
                )}
              </div>
              {/* 댓글은 마크다운이 아니라 글자 그대로 보여준다. 줄바꿈만 살린다. */}
              <p className="mt-2 text-sm leading-6 wrap-anywhere whitespace-pre-wrap">
                {comment.content}
              </p>
            </li>
          ))}
        </ul>
      )}

      <CommentForm
        action={createComment.bind(null, postId)}
        isAdmin={Boolean(admin)}
      />
    </section>
  );
}

export function CommentsSkeleton() {
  return (
    <div
      aria-hidden
      className="mt-12 animate-pulse border-t border-zinc-200 pt-8 dark:border-zinc-800"
    >
      <div className="h-6 w-20 rounded bg-zinc-200 dark:bg-zinc-800" />
      <div className="mt-6 h-24 rounded-md bg-zinc-200 dark:bg-zinc-800" />
    </div>
  );
}
