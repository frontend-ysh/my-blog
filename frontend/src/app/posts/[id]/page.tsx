import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { Comments, CommentsSkeleton } from "@/components/comments";
import { DeleteButton } from "@/components/delete-button";
import { Markdown } from "@/components/markdown";
import { TagLink } from "@/components/tag-link";
import { excerpt, formatDate } from "@/lib/format";
import { getPost } from "@/lib/posts";
import { getAdmin } from "@/lib/session";
import { deletePost } from "../actions";

/**
 * 브라우저 탭과 검색 결과에 보일 제목·설명을 글 내용으로 만든다.
 * 여기서도 getPost를 부르지만 cache() 덕분에 백엔드 요청은 한 번만 나간다.
 */
export async function generateMetadata({
  params,
}: PageProps<"/posts/[id]">): Promise<Metadata> {
  const { id } = await params;
  const post = await getPost(id);
  return {
    title: post.title,
    description: excerpt(post.content),
  };
}

export default function PostPage({ params }: PageProps<"/posts/[id]">) {
  return (
    // 주소의 [id]는 요청이 와야 알 수 있는 값이다. 그 값을 읽는 부분을 Suspense 안에 둔다.
    <Suspense fallback={<PostSkeleton />}>
      <PostArticle params={params} />
    </Suspense>
  );
}

async function PostArticle({ params }: Pick<PageProps<"/posts/[id]">, "params">) {
  const { id } = await params;
  // 글과 로그인 상태를 동시에 확인한다 (하나씩 기다리면 그만큼 느려진다).
  const [post, admin] = await Promise.all([getPost(id), getAdmin()]);
  const edited = post.updatedAt !== post.createdAt;

  return (
    <article>
      <header>
        <h1 className="text-3xl font-bold tracking-tight wrap-anywhere">
          {post.title}
        </h1>
        <p className="mt-3 text-sm text-zinc-500">
          <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
          {edited && (
            <>
              {" · "}
              <time dateTime={post.updatedAt}>
                {formatDate(post.updatedAt)} 수정
              </time>
            </>
          )}
        </p>
        {post.tags.length > 0 && (
          <ul className="mt-4 flex flex-wrap gap-2">
            {post.tags.map((tag) => (
              <li key={tag}>
                <TagLink name={tag} />
              </li>
            ))}
          </ul>
        )}
      </header>

      <div className="mt-8">
        <Markdown>{post.content}</Markdown>
      </div>

      <footer className="mt-12 flex items-center justify-between border-t border-zinc-200 pt-6 dark:border-zinc-800">
        <Link
          href="/"
          className="text-sm text-zinc-600 hover:underline hover:underline-offset-4 dark:text-zinc-400"
        >
          ← 목록
        </Link>
        {/* 수정·삭제 버튼은 관리자에게만 보인다. */}
        {admin && (
          <div className="flex items-center gap-1">
            <Link
              href={`/posts/${post.id}/edit`}
              className="rounded-md px-3 py-1.5 text-sm hover:bg-zinc-100 dark:hover:bg-zinc-900"
            >
              수정
            </Link>
            {/* bind로 "어느 글을 지울지"를 액션에 미리 묶어서 넘긴다. */}
            <DeleteButton action={deletePost.bind(null, post.id)} />
          </div>
        )}
      </footer>

      {/* 댓글은 글과 따로 불러온다. 글이 먼저 보이고, 댓글은 준비되는 대로 그 자리에 채워진다. */}
      <Suspense fallback={<CommentsSkeleton />}>
        <Comments postId={post.id} />
      </Suspense>
    </article>
  );
}

function PostSkeleton() {
  return (
    <div aria-hidden className="animate-pulse">
      <div className="h-9 w-3/4 rounded bg-zinc-200 dark:bg-zinc-800" />
      <div className="mt-4 h-4 w-32 rounded bg-zinc-200 dark:bg-zinc-800" />
      <div className="mt-10 space-y-3">
        <div className="h-4 rounded bg-zinc-200 dark:bg-zinc-800" />
        <div className="h-4 rounded bg-zinc-200 dark:bg-zinc-800" />
        <div className="h-4 w-5/6 rounded bg-zinc-200 dark:bg-zinc-800" />
      </div>
    </div>
  );
}
