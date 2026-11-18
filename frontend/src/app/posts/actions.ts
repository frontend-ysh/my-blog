"use server";

// 이 파일의 함수들은 "서버 액션"이다. 브라우저의 폼이 제출되면 Next.js 서버에서 실행된다.
// 화면에 버튼이 없어도 누구나 직접 POST 요청으로 호출할 수 있으므로,
// 인자는 타입 선언을 믿지 말고 항상 검증한다.
//
// 권한 검사는 백엔드가 한다. 여기서는 쿠키의 토큰을 실어 보내고, 백엔드가
// 401(로그인 필요)이나 403(권한 없음)으로 답하면 그에 맞게 안내한다.
// 화면에서 버튼을 숨기는 것은 편의일 뿐 보안이 아니다.

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { apiFetch, apiSend, type ProblemDetail } from "@/lib/api";
import type { PostFormState, PostFormValues } from "@/lib/post-form-state";
import type { Post } from "@/lib/posts";
import { getSessionToken } from "@/lib/session";

export async function createPost(
  _prevState: PostFormState,
  formData: FormData,
): Promise<PostFormState> {
  const values = readValues(formData);

  const result = await save("POST", "/api/posts", values);
  if (!result.ok) {
    return { values, ...result.error };
  }

  // 목록에 새 글이 생겼으니, 브라우저가 기억해 둔 예전 목록 화면을 버리게 한다.
  revalidatePath("/");
  // redirect()는 내부적으로 예외를 던져 흐름을 끊는다. try/catch 안에 두면 안 된다.
  redirect(`/posts/${result.post.id}`);
}

export async function updatePost(
  id: number,
  _prevState: PostFormState,
  formData: FormData,
): Promise<PostFormState> {
  const values = readValues(formData);

  const result = await save("PUT", `/api/posts/${toPostId(id)}`, values);
  if (!result.ok) {
    return { values, ...result.error };
  }

  // 이 글을 보여주는 화면 세 곳(목록, 상세, 수정 폼)의 예전 데이터를 버리게 한다.
  revalidatePath("/");
  revalidatePath(`/posts/${id}`);
  revalidatePath(`/posts/${id}/edit`);
  redirect(`/posts/${id}`);
}

export async function deletePost(id: number): Promise<void> {
  const postId = toPostId(id);
  const token = await getSessionToken();
  if (!token) {
    redirect(`/login?next=${encodeURIComponent(`/posts/${postId}`)}`);
  }

  const res = await apiFetch(`/api/posts/${postId}`, { method: "DELETE", token });
  if (res.status === 401) {
    redirect(`/login?next=${encodeURIComponent(`/posts/${postId}`)}`);
  }
  // 404는 이미 지워진 것이므로 결과적으로 성공과 같다.
  if (!res.ok && res.status !== 404) {
    throw new Error(`글을 삭제하지 못했습니다. (HTTP ${res.status})`);
  }

  revalidatePath("/");
  revalidatePath(`/posts/${postId}`);
  redirect("/");
}

type SaveResult =
  | { ok: true; post: Post }
  | { ok: false; error: Omit<PostFormState, "values"> };

/** 백엔드에 저장을 요청하고, 실패하면 폼에 보여줄 오류로 바꿔 돌려준다. */
async function save(
  method: "POST" | "PUT",
  path: string,
  values: PostFormValues,
): Promise<SaveResult> {
  const token = await getSessionToken();
  if (!token) {
    return loginRequired();
  }

  let res: Response;
  try {
    res = await apiSend(
      method,
      path,
      { title: values.title, content: values.content, tags: parseTags(values.tags) },
      token,
    );
  } catch {
    // 백엔드가 꺼져 있거나 응답이 없는 경우.
    return failure("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
  }

  if (res.ok) {
    return { ok: true, post: await res.json() };
  }

  const problem: ProblemDetail | null = await res.json().catch(() => null);

  if (res.status === 400 && problem?.errors?.length) {
    const fieldErrors: PostFormState["fieldErrors"] = {};
    for (const { field, message } of problem.errors) {
      // 백엔드는 몇 번째 태그가 틀렸는지 "tags[1]"처럼 알려준다. 입력칸은 하나이므로 "tags"로 모은다.
      const name = field.startsWith("tags") ? "tags" : field;
      if (
        (name === "title" || name === "content" || name === "tags") &&
        !fieldErrors[name]
      ) {
        fieldErrors[name] = message;
      }
    }
    if (Object.keys(fieldErrors).length > 0) {
      return { ok: false, error: { fieldErrors } };
    }
  }
  if (res.status === 401) {
    return loginRequired();
  }
  if (res.status === 403) {
    return failure("글을 저장할 권한이 없습니다.");
  }
  if (res.status === 404) {
    return failure("글을 찾을 수 없습니다. 이미 삭제되었을 수 있습니다.");
  }
  return failure("저장하지 못했습니다. 잠시 후 다시 시도해 주세요.");
}

function failure(message: string): SaveResult {
  return { ok: false, error: { fieldErrors: {}, message } };
}

/**
 * 로그인이 풀린 상태(쿠키가 없거나 토큰이 만료됨). 로그인 화면으로 보내지 않고
 * 폼에 안내를 띄운다. 화면을 옮기면 쓰던 글이 사라지기 때문이다.
 */
function loginRequired(): SaveResult {
  return {
    ok: false,
    error: {
      fieldErrors: {},
      message: "로그인이 만료되었습니다. 다시 로그인한 뒤 저장해 주세요.",
      loginRequired: true,
    },
  };
}

function readValues(formData: FormData): PostFormValues {
  return {
    title: readText(formData, "title").trim(),
    // 브라우저는 폼을 보낼 때 줄바꿈을 \r\n으로 바꾼다. \n으로 통일해서 저장한다.
    content: readText(formData, "content").replace(/\r\n?/g, "\n"),
    tags: readText(formData, "tags").trim(),
  };
}

/** "spring, jpa ,, docker" → ["spring", "jpa", "docker"]. 표기 통일과 중복 제거는 백엔드가 한다. */
function parseTags(input: string): string[] {
  return input
    .split(",")
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0);
}

function readText(formData: FormData, name: string): string {
  const value = formData.get(name);
  return typeof value === "string" ? value : "";
}

/**
 * id가 양의 정수인지 확인한다. 이 값은 백엔드 주소의 일부가 되므로,
 * 검증 없이 쓰면 조작된 값("1/../다른경로")으로 엉뚱한 API를 호출하게 만들 수 있다.
 */
function toPostId(id: unknown): number {
  if (typeof id !== "number" || !Number.isSafeInteger(id) || id <= 0) {
    throw new Error("잘못된 글 번호입니다.");
  }
  return id;
}
