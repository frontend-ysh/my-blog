"use client";

type Props = {
  /** 삭제를 실행할 서버 액션. */
  action: () => Promise<void>;
};

/** 확인 창을 띄운 뒤 삭제한다. 확인 창(confirm)은 브라우저 기능이라 클라이언트 컴포넌트다. */
export function DeleteButton({ action }: Props) {
  return (
    <form
      action={action}
      onSubmit={(event) => {
        if (!window.confirm("이 글을 삭제할까요? 되돌릴 수 없습니다.")) {
          // 제출을 취소하면 서버 액션도 실행되지 않는다.
          event.preventDefault();
        }
      }}
    >
      <button
        type="submit"
        className="rounded-md px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950"
      >
        삭제
      </button>
    </form>
  );
}
