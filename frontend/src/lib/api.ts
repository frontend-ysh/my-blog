// 이 파일을 브라우저용 코드(클라이언트 컴포넌트)에서 import하면 빌드가 실패한다.
// 백엔드 주소 같은 서버 전용 정보가 실수로 브라우저에 실려 나가는 것을 막는 안전장치다.
import "server-only";

/**
 * 백엔드(Spring Boot) API 주소.
 * NEXT_PUBLIC_ 접두사가 없는 환경변수는 Next.js 서버에서만 읽을 수 있다.
 */
const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

/** 백엔드의 오류 응답 형식 (Problem Details, RFC 9457). */
export type ProblemDetail = {
  title?: string;
  status?: number;
  detail?: string;
  errors?: { field: string; message: string }[];
};

type ApiInit = Omit<RequestInit, "signal"> & {
  /** 응답을 기다리는 최대 시간(ms). 기본 5초. 큰 파일을 주고받을 때만 늘린다. */
  timeoutMs?: number;
  /**
   * 로그인 토큰. 넘기면 Authorization 헤더에 실어 보낸다.
   * 로그인이 필요한 요청에만 넘긴다. 백엔드는 토큰이 실려 오면 공개 API라도 검증하므로,
   * 만료된 토큰을 목록 조회 같은 요청에 붙이면 볼 수 있던 것도 401이 된다.
   */
  token?: string;
};

/**
 * 백엔드 API를 호출한다. 정해진 시간(기본 5초) 안에 응답이 끝나지 않으면 포기하고 예외를 던진다.
 * (시간제한이 없으면 백엔드가 멈췄을 때 화면도 끝없이 기다리게 된다.)
 */
export function apiFetch(
  path: string,
  { token, headers, timeoutMs = 5000, ...init }: ApiInit = {},
): Promise<Response> {
  const requestHeaders = new Headers(headers);
  if (token) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }
  return fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: requestHeaders,
    signal: AbortSignal.timeout(timeoutMs),
  });
}

/** JSON 본문을 실어 보내는 요청(POST, PUT). */
export function apiSend(
  method: "POST" | "PUT",
  path: string,
  body: unknown,
  token?: string,
): Promise<Response> {
  return apiFetch(path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    token,
  });
}
