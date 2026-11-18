/** 글 목록 주소(/?q=검색어&tag=spring&page=2)의 조건. */
export type ListParams = {
  page: number;
  tag?: string;
  /** 검색어 */
  q?: string;
};

type RawSearchParams = Record<string, string | string[] | undefined>;

/**
 * 주소창의 값을 목록 조건으로 바꾼다. 주소는 누구나 마음대로 고칠 수 있으므로,
 * 이상한 값(?page=abc, ?page=-1)은 오류를 내지 않고 기본값으로 돌린다.
 */
export function parseListParams(searchParams: RawSearchParams): ListParams {
  const rawPage = first(searchParams.page);
  // 백엔드가 태그를 저장할 때와 같은 표기(소문자, 공백 한 칸)로 맞춘다.
  // ?tag=Spring 으로 들어와도 화면에는 저장된 이름인 #spring 으로 보인다.
  const rawTag = first(searchParams.tag)?.trim().toLowerCase().replace(/\s+/g, " ");

  const rawQuery = first(searchParams.q)?.trim();

  return {
    page: rawPage && /^[1-9]\d{0,5}$/.test(rawPage) ? Number(rawPage) : 1,
    tag: rawTag && rawTag.length <= 30 ? rawTag : undefined,
    q: rawQuery && rawQuery.length <= 100 ? rawQuery : undefined,
  };
}

/** 목록 조건을 주소로 바꾼다. 기본값(1페이지, 태그·검색어 없음)은 주소에 넣지 않는다. */
export function listHref({ page = 1, tag, q }: Partial<ListParams>): string {
  const params = new URLSearchParams();
  if (q) {
    params.set("q", q);
  }
  if (tag) {
    params.set("tag", tag);
  }
  if (page > 1) {
    params.set("page", String(page));
  }
  const query = params.toString();
  return query ? `/?${query}` : "/";
}

/** ?tag=a&tag=b처럼 같은 이름이 여러 번 오면 배열이 된다. 첫 값만 쓴다. */
function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
