"use server";

import { revalidatePath } from "next/cache";
import { apiFetch, apiSend, type ProblemDetail } from "@/lib/api";
import type {
  CommentDeleteState,
  CommentFormState,
} from "@/lib/comment-form-state";
import { getAdmin, getSessionToken } from "@/lib/session";

const CONNECTION_ERROR = "서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.";

export async function createComment(
  postId: number,
  _prevState: CommentFormState,
  formData: FormData,
): Promise<CommentFormState> {
  const id = toId(postId);
  const values = {
    authorName: readText(formData, "authorName").trim(),
    content: readText(formData, "content").replace(/\r\n?/g, "\n"),
  };
  const fail = (
    error: Omit<CommentFormState, "values">,
  ): CommentFormState => ({ values, ...error });

  // 관리자로 로그인했으면 토큰을 실어 보낸다. 백엔드가 토큰을 보고 "글쓴이" 댓글로 저장한다.
  // 폼에서 넘어온 값으로 관리자 여부를 정하지 않는다. 폼의 값은 누구나 바꿔 보낼 수 있다.
  const admin = await getAdmin();

  let res: Response;
  try {
    res = admin
      ? await apiSend(
          "POST",
          `/api/posts/${id}/comments`,
          { content: values.content },
          await getSessionToken(),
        )
      : await apiSend("POST", `/api/posts/${id}/comments`, {
          authorName: values.authorName,
          password: readText(formData, "password"),
          content: values.content,
        });
  } catch {
    return fail({ fieldErrors: {}, message: CONNECTION_ERROR });
  }

  if (!res.ok) {
    const problem: ProblemDetail | null = await res.json().catch(() => null);
    if (res.status === 400 && problem?.errors?.length) {
      const fieldErrors: CommentFormState["fieldErrors"] = {};
      for (const { field, message } of problem.errors) {
        if (
          (field === "authorName" || field === "password" || field === "content") &&
          !fieldErrors[field]
        ) {
          fieldErrors[field] = message;
        }
      }
      return fail({ fieldErrors });
    }
    if (res.status === 401) {
      return fail({
        fieldErrors: {},
        message: "로그인이 만료되었습니다. 다시 로그인해 주세요.",
      });
    }
    if (res.status === 404) {
      return fail({ fieldErrors: {}, message: "글을 찾을 수 없습니다." });
    }
    return fail({
      fieldErrors: {},
      message: "댓글을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.",
    });
  }

  // 화면을 옮기지 않는다. 이 글 화면의 데이터를 새로 받게 해서 방금 쓴 댓글이 목록에 나타나게 한다.
  revalidatePath(`/posts/${id}`);
  // 내용만 비우고 이름은 남겨 둔다(이어서 또 쓸 때 편하다).
  return {
    values: { authorName: values.authorName, content: "" },
    fieldErrors: {},
  };
}

export async function deleteComment(
  postId: number,
  commentId: number,
  _prevState: CommentDeleteState,
  formData: FormData,
): Promise<CommentDeleteState> {
  const id = toId(commentId);
  const admin = await getAdmin();

  let res: Response;
  try {
    res = admin
      ? await apiFetch(`/api/comments/${id}`, {
          method: "DELETE",
          token: await getSessionToken(),
        })
      : await apiFetch(`/api/comments/${id}`, {
          method: "DELETE",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ password: readText(formData, "password") }),
        });
  } catch {
    return { message: CONNECTION_ERROR };
  }

  if (res.status === 403) {
    return { message: "비밀번호가 올바르지 않습니다." };
  }
  // 404는 이미 지워진 것이므로 결과적으로 성공과 같다.
  if (!res.ok && res.status !== 404) {
    return { message: "댓글을 지우지 못했습니다. 잠시 후 다시 시도해 주세요." };
  }

  revalidatePath(`/posts/${toId(postId)}`);
  return {};
}

function readText(formData: FormData, name: string): string {
  const value = formData.get(name);
  return typeof value === "string" ? value : "";
}

/** 주소의 일부가 되는 번호다. 양의 정수가 아니면 받지 않는다. */
function toId(id: unknown): number {
  if (typeof id !== "number" || !Number.isSafeInteger(id) || id <= 0) {
    throw new Error("잘못된 번호입니다.");
  }
  return id;
}
