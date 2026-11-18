"use server";

import { apiFetch, type ProblemDetail } from "@/lib/api";
import { getSessionToken } from "@/lib/session";

/** 성공하면 본문에 넣을 이미지 주소, 실패하면 화면에 보여줄 문구. */
export type UploadResult = { url: string } | { error: string };

const MAX_BYTES = 5 * 1024 * 1024;
const LOGIN_REQUIRED = "로그인이 만료되었습니다. 다시 로그인한 뒤 시도해 주세요.";

/**
 * 이미지 하나를 백엔드에 올린다. 폼 제출이 아니라 "이미지 첨부" 버튼의 이벤트에서 직접 부른다.
 * 파일이 진짜 이미지인지는 백엔드가 내용을 보고 판단한다. 여기서는 전달만 한다.
 */
export async function uploadImage(formData: FormData): Promise<UploadResult> {
  const token = await getSessionToken();
  if (!token) {
    return { error: LOGIN_REQUIRED };
  }

  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) {
    return { error: "파일을 선택해 주세요." };
  }
  if (file.size > MAX_BYTES) {
    return { error: "이미지는 5MB 이하여야 합니다." };
  }

  // 파일은 JSON이 아니라 multipart/form-data로 보낸다. FormData를 본문으로 주면 fetch가 알아서 그 형식으로 만든다.
  const body = new FormData();
  body.set("file", file);

  let res: Response;
  try {
    res = await apiFetch("/api/images", {
      method: "POST",
      body,
      token,
      timeoutMs: 30_000,
    });
  } catch {
    return { error: "서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요." };
  }

  if (res.ok) {
    const { url }: { url: string } = await res.json();
    return { url };
  }

  const problem: ProblemDetail | null = await res.json().catch(() => null);
  if (res.status === 400 && problem?.errors?.[0]) {
    return { error: problem.errors[0].message };
  }
  if (res.status === 401) {
    return { error: LOGIN_REQUIRED };
  }
  if (res.status === 403) {
    return { error: "이미지를 올릴 권한이 없습니다." };
  }
  if (res.status === 413) {
    return { error: "이미지는 5MB 이하여야 합니다." };
  }
  return { error: "이미지를 올리지 못했습니다. 잠시 후 다시 시도해 주세요." };
}
