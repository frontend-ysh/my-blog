import "server-only";

import { apiFetch } from "./api";

/** 댓글. 백엔드의 CommentResponse와 같은 모양이다. 비밀번호는 서버 밖으로 나오지 않는다. */
export type Comment = {
  id: number;
  authorName: string;
  content: string;
  /** 관리자(글쓴이)가 쓴 댓글인지 */
  byAdmin: boolean;
  createdAt: string;
};

export async function getComments(postId: number): Promise<Comment[]> {
  const res = await apiFetch(`/api/posts/${postId}/comments`);
  if (!res.ok) {
    throw new Error(`댓글을 불러오지 못했습니다. (HTTP ${res.status})`);
  }
  return res.json();
}
