import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { Pagination } from "@/components/pagination";
import { SearchForm } from "@/components/search-form";
import { TagLink } from "@/components/tag-link";
import { formatDate } from "@/lib/format";
import { listHref, parseListParams } from "@/lib/list-params";
import { getPosts, getTags } from "@/lib/posts";
import { getAdmin } from "@/lib/session";

/**
 * 검색 중이거나 태그로 걸러 보는 중이면 탭 제목을 "#태그 | Blog"처럼 바꾼다.
 * layout.tsx의 제목 틀("%s | Blog")은 하위 경로의 페이지에만 적용되고, layout과 같은 자리에 있는
 * 이 페이지에는 적용되지 않는다. 그래서 여기서는 전체 제목을 직접 적는다.
 */
export async function generateMetadata({
  searchParams,
}: PageProps<"/">): Promise<Metadata> {
  const { tag, q } = parseListParams(await searchParams);
  if (q) {
    return {
      title: `"${q}" 검색 | Blog`,
      // 검색어마다 주소가 생기므로 검색 결과 화면은 검색엔진에 올리지 않는다.
      robots: { index: false },
    };
  }
  return tag ? { title: `#${tag} | Blog` } : {};
}

export default function Home({ searchParams }: PageProps<"/">) {
  return (
    // 주소의 ?tag=, ?page= 는 요청이 와야 알 수 있는 값이다. 그 값을 읽는 부분을 Suspense 안에 둔다.
    // 머리말은 즉시 보내고, 목록은 백엔드 응답이 오는 대로 채운다. 그동안은 뼈대 화면이 보인다.
    <Suspense fallback={<PostListSkeleton />}>
      <PostList searchParams={searchParams} />
    </Suspense>
  );
}

/** 서버에서 실행되는 컴포넌트. 백엔드에서 목록을 받아 HTML로 만든다. */
async function PostList({ searchParams }: Pick<PageProps<"/">, "searchParams">) {
  const { page, tag, q } = parseListParams(await searchParams);
  // 글 목록과 태그 목록을 동시에 요청한다.
  const [posts, tags] = await Promise.all([
    getPosts({ page, tag, q }),
    getTags(),
  ]);
  const heading = [tag && `#${tag}`, q && `"${q}" 검색 결과`]
    .filter(Boolean)
    .join(" · ");

  return (
    <>
      <div className="flex items-baseline justify-between gap-4">
        <h1 className="text-2xl font-bold tracking-tight wrap-anywhere">
          {heading || "최근 글"}
        </h1>
        <p className="shrink-0 text-sm text-zinc-500">
          글 {posts.totalElements}개
        </p>
      </div>

      <SearchForm q={q} />

      {tags.length > 0 && (
        <nav aria-label="태그" className="mt-4 flex flex-wrap gap-2">
          {(tag || q) && (
            <Link
              href="/"
              className="rounded-full border border-zinc-300 px-2.5 py-0.5 text-xs text-zinc-600 hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-400 dark:hover:bg-zinc-900"
            >
              전체 보기
            </Link>
          )}
          {tags.map(({ name, postCount }) => (
            <TagLink
              key={name}
              name={name}
              count={postCount}
              active={name === tag}
            />
          ))}
        </nav>
      )}

      {posts.content.length === 0 ? (
        <EmptyState
          filtered={Boolean(tag || q)}
          hasPostsOnOtherPages={posts.totalElements > 0}
          firstPageHref={listHref({ tag, q })}
        />
      ) : (
        <ul className="mt-4 divide-y divide-zinc-200 dark:divide-zinc-800">
          {posts.content.map((post) => (
            <li key={post.id} className="py-5">
              <Link href={`/posts/${post.id}`} className="group block">
                <h2 className="text-lg font-semibold wrap-anywhere group-hover:underline group-hover:underline-offset-4">
                  {post.title}
                </h2>
              </Link>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <time dateTime={post.createdAt} className="text-sm text-zinc-500">
                  {formatDate(post.createdAt)}
                </time>
                {post.tags.map((name) => (
                  <TagLink key={name} name={name} />
                ))}
              </div>
            </li>
          ))}
        </ul>
      )}

      <Pagination
        page={posts.page}
        totalPages={posts.totalPages}
        hrefFor={(target) => listHref({ page: target, tag, q })}
      />
    </>
  );
}

async function EmptyState({
  filtered,
  hasPostsOnOtherPages,
  firstPageHref,
}: {
  /** 검색어나 태그로 걸러 보는 중인지 */
  filtered: boolean;
  hasPostsOnOtherPages: boolean;
  firstPageHref: string;
}) {
  const boxClass =
    "mt-8 rounded-lg border border-dashed border-zinc-300 px-6 py-12 text-center dark:border-zinc-700";
  const linkClass =
    "mt-4 inline-block text-sm font-medium underline underline-offset-4";

  // 주소의 페이지 번호가 마지막 페이지보다 큰 경우 (예: 글이 지워져 페이지 수가 줄었다).
  if (hasPostsOnOtherPages) {
    return (
      <div className={boxClass}>
        <p className="text-zinc-600 dark:text-zinc-400">
          이 페이지에는 글이 없습니다.
        </p>
        <Link href={firstPageHref} className={linkClass}>
          첫 페이지로
        </Link>
      </div>
    );
  }

  if (filtered) {
    return (
      <div className={boxClass}>
        <p className="text-zinc-600 dark:text-zinc-400">
          조건에 맞는 글이 없습니다.
        </p>
        <Link href="/" className={linkClass}>
          전체 글 보기
        </Link>
      </div>
    );
  }

  const admin = await getAdmin();
  return (
    <div className={boxClass}>
      <p className="text-zinc-600 dark:text-zinc-400">아직 글이 없습니다.</p>
      {admin && (
        <Link href="/posts/new" className={linkClass}>
          첫 글 쓰기
        </Link>
      )}
    </div>
  );
}

function PostListSkeleton() {
  return (
    <div aria-hidden className="animate-pulse">
      <div className="h-8 w-28 rounded bg-zinc-200 dark:bg-zinc-800" />
      <ul className="mt-4 divide-y divide-zinc-200 dark:divide-zinc-800">
        {[0, 1, 2].map((i) => (
          <li key={i} className="py-5">
            <div className="h-6 w-2/3 rounded bg-zinc-200 dark:bg-zinc-800" />
            <div className="mt-2 h-4 w-28 rounded bg-zinc-200 dark:bg-zinc-800" />
          </li>
        ))}
      </ul>
    </div>
  );
}
