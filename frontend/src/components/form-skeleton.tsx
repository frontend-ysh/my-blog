/** 폼이 준비되기 전(로그인 확인, 기존 글 불러오기)에 잠깐 보이는 뼈대 화면. */
export function FormSkeleton({ tall = false }: { tall?: boolean }) {
  return (
    <div aria-hidden className="animate-pulse space-y-5">
      <div className="h-10 rounded-md bg-zinc-200 dark:bg-zinc-800" />
      <div
        className={`rounded-md bg-zinc-200 dark:bg-zinc-800 ${tall ? "h-96" : "h-10"}`}
      />
    </div>
  );
}
