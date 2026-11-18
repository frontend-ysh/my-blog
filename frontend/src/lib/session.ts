import "server-only";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { cache } from "react";
import { apiFetch } from "./api";

// 로그인 상태를 다루는 곳. 로그인하면 백엔드가 준 토큰(JWT)을 쿠키에 넣어 두고,
// 이후 요청마다 그 쿠키에서 토큰을 꺼내 백엔드로 전달한다.
//
//   브라우저 ──(쿠키)──▶ Next.js 서버 ──(Authorization: Bearer 토큰)──▶ Spring Boot
//
// 토큰이 진짜인지 판단하는 것은 언제나 백엔드다. 여기서는 전달만 한다.

const SESSION_COOKIE = "blog_session";

/** 로그인한 관리자. 화면에 필요한 최소한의 정보만 담는다. */
export type Admin = {
  username: string;
};

/** 쿠키에 들어 있는 토큰. 없으면 undefined. */
export async function getSessionToken(): Promise<string | undefined> {
  return (await cookies()).get(SESSION_COOKIE)?.value;
}

/**
 * 지금 방문자가 로그인한 관리자인지 백엔드에 물어본다. 아니면 null.
 *
 * 쿠키가 없는 일반 방문자는 백엔드를 호출하지 않고 바로 null을 돌려준다.
 * cache()로 감싸서, 한 번의 페이지 요청 안에서 머리말과 본문이 각각 불러도 호출은 한 번이다.
 */
export const getAdmin = cache(async (): Promise<Admin | null> => {
  const token = await getSessionToken();
  if (!token) {
    return null;
  }

  let res: Response;
  try {
    res = await apiFetch("/api/auth/me", { token });
  } catch {
    // 백엔드에 닿지 못해도 화면은 계속 나와야 한다. 로그인하지 않은 것으로 본다.
    return null;
  }
  if (!res.ok) {
    // 401: 토큰이 만료되었거나 유효하지 않다.
    return null;
  }

  const me: { username: string; roles: string[] } = await res.json();
  return me.roles.includes("ADMIN") ? { username: me.username } : null;
});

/**
 * 관리자만 볼 수 있는 화면에서 부른다. 관리자가 아니면 로그인 화면으로 보내고,
 * 로그인한 뒤에는 원래 가려던 주소(next)로 돌아오게 한다.
 */
export async function requireAdmin(next: string): Promise<Admin> {
  const admin = await getAdmin();
  if (!admin) {
    redirect(`/login?next=${encodeURIComponent(next)}`);
  }
  return admin;
}

/** 로그인 성공 후 토큰을 쿠키에 저장한다. 쿠키는 서버 액션 안에서만 쓸 수 있다. */
export async function createSession(
  token: string,
  maxAgeSeconds: number,
): Promise<void> {
  (await cookies()).set(SESSION_COOKIE, token, {
    // 브라우저의 자바스크립트가 읽을 수 없다. 페이지에 악성 스크립트가 끼어들어도 토큰을 훔쳐 가지 못한다.
    httpOnly: true,
    // HTTPS로만 보낸다. 개발 중(http://localhost)에는 꺼 둔다.
    secure: process.env.NODE_ENV === "production",
    // 다른 사이트에서 몰래 보내는 요청(폼 전송 등)에는 쿠키가 실리지 않는다 (CSRF 방어).
    sameSite: "lax",
    path: "/",
    // 토큰이 만료될 때 쿠키도 함께 사라지게 한다.
    maxAge: maxAgeSeconds,
  });
}

export async function deleteSession(): Promise<void> {
  (await cookies()).delete(SESSION_COOKIE);
}
