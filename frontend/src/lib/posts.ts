import "server-only";

import { notFound } from "next/navigation";
import { cache } from "react";
import { apiFetch } from "./api";

/** 목록의 한 항목. 백엔드의 PostSummaryResponse와 같은 모양이다. */
export type PostSummary = {
  id: number;
  title: string;
  tags: string[];
  createdAt: string;
};

/** 글 상세. 백엔드의 PostResponse와 같은 모양이다. */
export type Post = {
  id: number;
  title: string;
  content: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
};

/** 목록의 한 페이지. 백엔드의 PageResponse와 같은 모양이다. */
export type PostPage = {
  content: PostSummary[];
  /** 현재 페이지 (1부터 시작) */
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/** 태그와, 그 태그가 붙은 글의 수. */
export type TagCount = {
  name: string;
  postCount: number;
};

export async function getPosts({
  page,
  tag,
  q,
}: {
  page: number;
  tag?: string;
  q?: string;
}): Promise<PostPage> {
  // URLSearchParams가 한글·공백·특수문자를 주소에 넣을 수 있는 형태로 바꿔 준다.
  const params = new URLSearchParams({ page: String(page) });
  if (tag) {
    params.set("tag", tag);
  }
  if (q) {
    params.set("q", q);
  }

  const res = await apiFetch(`/api/posts?${params}`);
  if (!res.ok) {
    throw new Error(`글 목록을 불러오지 못했습니다. (HTTP ${res.status})`);
  }
  return res.json();
}

export async function getTags(): Promise<TagCount[]> {
  const res = await apiFetch("/api/tags");
  if (!res.ok) {
    throw new Error(`태그 목록을 불러오지 못했습니다. (HTTP ${res.status})`);
  }
  return res.json();
}

/**
 * 글 하나를 가져온다. 없으면 notFound()로 404 화면을 띄운다.
 *
 * cache()로 감싼 이유: 한 번의 페이지 요청 안에서 제목 태그(generateMetadata)와
 * 본문 컴포넌트가 같은 글을 각각 요청한다. cache()는 같은 id에 대한 호출을
 * 한 번으로 합쳐 준다. 요청이 끝나면 버려지므로 다음 방문자는 새로 조회한다.
 */
export const getPost = cache(async (id: string): Promise<Post> => {
  // 주소창에는 무엇이든 들어올 수 있다. 숫자가 아니면 백엔드에 묻지 않고 바로 404.
  if (!/^\d+$/.test(id)) {
    notFound();
  }

  const res = await apiFetch(`/api/posts/${id}`);
  if (res.status === 404) {
    notFound();
  }
  if (!res.ok) {
    throw new Error(`글을 불러오지 못했습니다. (HTTP ${res.status})`);
  }
  return res.json();
});
