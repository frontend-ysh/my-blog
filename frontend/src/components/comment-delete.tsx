"use client";

import { useActionState, useState } from "react";
import type { CommentDeleteState } from "@/lib/comment-form-state";
import { fieldClass } from "./form-styles";

type Props = {
  action: (
    state: CommentDeleteState,
    formData: FormData,
  ) => Promise<CommentDeleteState>;
  /** 관리자는 비밀번호 없이 지운다. */
  isAdmin: boolean;
};

const textButtonClass =
  "rounded px-1.5 py-0.5 text-xs text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-900";

/** 댓글의 "삭제". 방문자는 누르면 비밀번호 입력칸이 펼쳐지고, 관리자는 확인만 하고 지운다. */
export function CommentDelete({ action, isAdmin }: Props) {
  const [state, formAction, pending] = useActionState(action, {});
  const [open, setOpen] = useState(false);

  if (isAdmin) {
    return (
      <form
        action={formAction}
        onSubmit={(event) => {
          if (!window.confirm("이 댓글을 삭제할까요?")) {
            event.preventDefault();
          }
        }}
      >
        <button
          type="submit"
          disabled={pending}
          aria-label="댓글 삭제"
          className={textButtonClass}
        >
          삭제
        </button>
        {state.message && (
          <span role="alert" className="ml-2 text-xs text-red-600">
            {state.message}
          </span>
        )}
      </form>
    );
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        // 같은 화면에 글의 "삭제" 버튼도 있다. 화면 낭독기가 둘을 구분해 읽도록 이름을 따로 준다.
        aria-label="댓글 삭제"
        className={textButtonClass}
      >
        삭제
      </button>
    );
  }

  return (
    <form action={formAction} className="flex flex-wrap items-center gap-1.5">
      <input
        name="password"
        type="password"
        required
        autoFocus
        autoComplete="off"
        placeholder="비밀번호"
        aria-label="댓글 비밀번호"
        aria-invalid={state.message ? true : undefined}
        className={`${fieldClass} w-32 px-2 py-1 text-xs`}
      />
      <button type="submit" disabled={pending} className={textButtonClass}>
        {pending ? "삭제 중…" : "확인"}
      </button>
      <button
        type="button"
        onClick={() => setOpen(false)}
        className={textButtonClass}
      >
        취소
      </button>
      {state.message && (
        <span role="alert" className="text-xs text-red-600 dark:text-red-400">
          {state.message}
        </span>
      )}
    </form>
  );
}
