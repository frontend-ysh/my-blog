// 주소를 해석할 때만 쓰는 가짜 기준 주소. 실제로 접속하지 않는다.
const BASE = "http://internal.invalid";

/**
 * 로그인 후 돌아갈 주소를 검증한다. 우리 사이트 안의 경로만 허용하고, 아니면 홈("/")을 돌려준다.
 *
 * 이 값은 주소창(?next=...)에서 오므로 누구나 조작할 수 있다. 검증 없이 쓰면
 * "우리 사이트 로그인 링크"처럼 보이는 주소로 사용자를 가짜 사이트에 보낼 수 있다
 * (오픈 리다이렉트). "//evil.com"이나 "/\evil.com"처럼 경로처럼 보이지만
 * 브라우저가 다른 사이트로 해석하는 값까지 걸러야 하므로, 직접 문자열을 검사하지 않고
 * 브라우저와 같은 규칙으로 해석(URL)한 뒤 사이트가 바뀌었는지 본다.
 */
export function safeRedirectPath(value: string | null | undefined): string {
  if (!value || !value.startsWith("/")) {
    return "/";
  }
  try {
    const url = new URL(value, BASE);
    if (url.origin !== BASE) {
      return "/";
    }
    return url.pathname + url.search + url.hash;
  } catch {
    return "/";
  }
}
