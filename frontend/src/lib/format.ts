// 서버가 어느 나라에 있든 한국 시간 기준으로 날짜를 표시한다.
const dateFormatter = new Intl.DateTimeFormat("ko-KR", {
  dateStyle: "long",
  timeZone: "Asia/Seoul",
});

/** ISO 8601 문자열을 "2026년 11월 17일" 형식으로 바꾼다. */
export function formatDate(iso: string): string {
  return dateFormatter.format(new Date(iso));
}

const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
  dateStyle: "long",
  timeStyle: "short",
  timeZone: "Asia/Seoul",
});

/** ISO 8601 문자열을 "2026년 11월 17일 오후 3:51" 형식으로 바꾼다. */
export function formatDateTime(iso: string): string {
  return dateTimeFormatter.format(new Date(iso));
}

/**
 * 마크다운 본문에서 기호를 걷어 내고 앞부분만 잘라, 검색 결과에 보일 설명문을 만든다.
 * 정확한 마크다운 해석이 아니라, 자주 쓰는 기호만 지우는 간단한 처리다.
 */
export function excerpt(markdown: string, maxLength = 150): string {
  return markdown
    .replace(/```[^\n]*\n?/g, " ") // 코드 블록의 울타리(```js)
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, "$1") // 이미지 → 대체 텍스트
    .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1") // 링크 → 링크 글자
    .replace(/^\s{0,3}(#{1,6}|>|[-*+]|\d+\.)\s+/gm, "") // 줄 앞의 제목·인용·목록 기호
    .replace(/[*~`]/g, "") // 강조·취소선·인라인 코드 기호
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, maxLength);
}
