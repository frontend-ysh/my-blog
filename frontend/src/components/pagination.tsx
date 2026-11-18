import Link from "next/link";

type Props = {
  page: number;
  totalPages: number;
  /** 페이지 번호를 받아 그 페이지의 주소를 돌려준다. */
  hrefFor: (page: number) => string;
};

const itemClass =
  "flex h-9 min-w-9 items-center justify-center rounded-md px-3 text-sm";
const linkClass = `${itemClass} hover:bg-zinc-100 dark:hover:bg-zinc-900`;
const disabledClass = `${itemClass} text-zinc-400 dark:text-zinc-600`;

/** 목록 아래의 페이지 이동 메뉴. 페이지가 하나뿐이면 아무것도 그리지 않는다. */
export function Pagination({ page, totalPages, hrefFor }: Props) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav
      aria-label="페이지 이동"
      className="mt-8 flex flex-wrap items-center justify-center gap-1"
    >
      {page > 1 ? (
        <Link href={hrefFor(page - 1)} rel="prev" className={linkClass}>
          ← 이전
        </Link>
      ) : (
        <span className={disabledClass}>← 이전</span>
      )}

      {pageNumbers(page, totalPages).map((item, index) =>
        item === "gap" ? (
          <span key={`gap-${index}`} className={disabledClass}>
            …
          </span>
        ) : item === page ? (
          <span
            key={item}
            aria-current="page"
            className={`${itemClass} bg-foreground font-medium text-background`}
          >
            {item}
          </span>
        ) : (
          <Link key={item} href={hrefFor(item)} className={linkClass}>
            {item}
          </Link>
        ),
      )}

      {page < totalPages ? (
        <Link href={hrefFor(page + 1)} rel="next" className={linkClass}>
          다음 →
        </Link>
      ) : (
        <span className={disabledClass}>다음 →</span>
      )}
    </nav>
  );
}

/**
 * 보여줄 페이지 번호를 고른다. 처음, 끝, 현재 페이지와 그 앞뒤만 남기고 나머지는 줄임표("gap")로 묶는다.
 * 예: 현재 6, 전체 20 → 1 … 5 6 7 … 20
 */
function pageNumbers(page: number, totalPages: number): (number | "gap")[] {
  const wanted = [1, page - 1, page, page + 1, totalPages].filter(
    (n) => n >= 1 && n <= totalPages,
  );
  const sorted = [...new Set(wanted)].sort((a, b) => a - b);

  const result: (number | "gap")[] = [];
  sorted.forEach((n, index) => {
    const previous = sorted[index - 1];
    if (previous !== undefined && n - previous === 2) {
      // 사이에 숫자가 하나뿐이면 줄임표 대신 그 숫자를 보여준다.
      result.push(n - 1);
    } else if (previous !== undefined && n - previous > 2) {
      result.push("gap");
    }
    result.push(n);
  });
  return result;
}
