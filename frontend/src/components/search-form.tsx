import Form from "next/form";
import { fieldClass } from "./form-styles";

/**
 * 검색창. 제출하면 /?q=검색어 로 이동한다.
 *
 * 저장하는 폼이 아니라 "주소를 바꾸는" 폼이라서 서버 액션이 필요 없다. 검색 결과가 주소에 담기므로
 * 그 주소를 그대로 공유하거나 새로고침해도 같은 결과가 나온다.
 * next/form의 Form은 일반 <form>과 같지만, 페이지 전체를 새로 받지 않고 바뀐 부분만 받아 온다.
 */
export function SearchForm({ q }: { q?: string }) {
  return (
    <Form action="/" role="search" className="mt-4 flex gap-2">
      <input
        // 화면이 보관되었다가 다시 보일 때 입력칸에 예전 검색어가 남지 않도록,
        // 검색어가 바뀌면 입력칸을 새로 만든다.
        key={q ?? ""}
        type="search"
        name="q"
        defaultValue={q}
        maxLength={100}
        placeholder="제목·본문 검색"
        aria-label="검색어"
        className={`${fieldClass} py-1.5 text-sm`}
      />
      <button
        type="submit"
        className="shrink-0 rounded-md border border-zinc-300 px-3 py-1.5 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-900"
      >
        검색
      </button>
    </Form>
  );
}
