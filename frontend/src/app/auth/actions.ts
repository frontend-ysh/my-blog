"use server";

import { redirect } from "next/navigation";
import { apiSend } from "@/lib/api";
import type { LoginFormState } from "@/lib/login-form-state";
import { safeRedirectPath } from "@/lib/redirect";
import { createSession, deleteSession } from "@/lib/session";

export async function login(
  _prevState: LoginFormState,
  formData: FormData,
): Promise<LoginFormState> {
  const username = readText(formData, "username").trim();
  const password = readText(formData, "password");
  // 로그인 후 돌아갈 주소. 폼의 숨은 칸으로 넘어오므로 조작될 수 있다고 보고 다시 검증한다.
  const next = safeRedirectPath(readText(formData, "next"));

  // 실패하면 아이디는 다시 채워 주지만, 비밀번호는 절대 화면으로 되돌려 보내지 않는다.
  const fail = (message: string): LoginFormState => ({ username, message });

  if (!username || !password) {
    return fail("아이디와 비밀번호를 입력하세요.");
  }

  let res: Response;
  try {
    res = await apiSend("POST", "/api/auth/login", { username, password });
  } catch {
    return fail("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
  }

  if (res.status === 401 || res.status === 400) {
    return fail("아이디 또는 비밀번호가 올바르지 않습니다.");
  }
  if (!res.ok) {
    return fail("로그인하지 못했습니다. 잠시 후 다시 시도해 주세요.");
  }

  const { accessToken, expiresIn }: { accessToken: string; expiresIn: number } =
    await res.json();
  await createSession(accessToken, expiresIn);

  redirect(next);
}

/**
 * 쿠키를 지워 로그아웃한다.
 * 토큰 자체는 만료 시각까지 유효하다(JWT는 서버에서 무효로 만들 수 없다).
 * 다만 토큰은 이 쿠키에만 들어 있고 자바스크립트로 읽을 수 없으므로, 쿠키를 지우면 쓸 방법이 없어진다.
 */
export async function logout(): Promise<void> {
  await deleteSession();
  redirect("/");
}

function readText(formData: FormData, name: string): string {
  const value = formData.get(name);
  return typeof value === "string" ? value : "";
}
