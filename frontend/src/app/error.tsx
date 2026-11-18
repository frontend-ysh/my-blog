"use client"; // 오류 화면(에러 바운더리)은 반드시 클라이언트 컴포넌트여야 한다.

import { useEffect } from "react";

/**
 * 페이지를 그리다 예외가 나면 그 페이지 자리에 대신 보이는 화면.
 * 머리말 같은 공통 틀(layout)은 그대로 남는다.
 */
export default function ErrorPage({
  error,
  retry,
}: {
  error: unknown;
  retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="py-16 text-center">
      <h1 className="text-2xl font-bold tracking-tight">
        내용을 불러오지 못했습니다
      </h1>
      <p className="mt-3 text-zinc-600 dark:text-zinc-400">
        서버에 일시적인 문제가 있을 수 있습니다. 잠시 후 다시 시도해 주세요.
      </p>
      <button
        type="button"
        onClick={() => retry()}
        className="mt-6 rounded-md border border-zinc-300 px-4 py-2 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-900"
      >
        다시 시도
      </button>
    </div>
  );
}
