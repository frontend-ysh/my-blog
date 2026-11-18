import Link from "next/link";

/** 없는 주소로 들어왔거나, 코드에서 notFound()를 불렀을 때 보이는 화면. */
export default function NotFound() {
  return (
    <div className="py-16 text-center">
      <h1 className="text-2xl font-bold tracking-tight">
        페이지를 찾을 수 없습니다
      </h1>
      <p className="mt-3 text-zinc-600 dark:text-zinc-400">
        주소가 잘못되었거나 삭제된 글입니다.
      </p>
      <Link
        href="/"
        className="mt-6 inline-block text-sm font-medium underline underline-offset-4"
      >
        목록으로 돌아가기
      </Link>
    </div>
  );
}
