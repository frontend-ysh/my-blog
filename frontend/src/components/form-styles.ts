/** 입력칸 공통 스타일. 글 폼과 로그인 폼이 함께 쓴다. */
export const fieldClass =
  "w-full rounded-md border border-zinc-300 bg-transparent px-3 py-2 outline-none " +
  "placeholder:text-zinc-400 focus:border-zinc-500 focus:ring-2 focus:ring-zinc-500/20 " +
  "aria-invalid:border-red-500 dark:border-zinc-700 dark:focus:border-zinc-400";

/** 폼 맨 위에 띄우는 오류 안내 상자. */
export const alertClass =
  "rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700 " +
  "dark:border-red-900 dark:bg-red-950 dark:text-red-300";

export const primaryButtonClass =
  "rounded-md bg-foreground px-4 py-2 text-sm font-medium text-background " +
  "hover:opacity-90 disabled:opacity-50";
