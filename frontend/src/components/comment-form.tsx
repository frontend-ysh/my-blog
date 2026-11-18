"use client";

import { useActionState } from "react";
import type { CommentFormState } from "@/lib/comment-form-state";
import { alertClass, fieldClass, primaryButtonClass } from "./form-styles";

type Props = {
  action: (
    state: CommentFormState,
    formData: FormData,
  ) => Promise<CommentFormState>;
  /** 관리자로 로그인했는지. 관리자는 이름과 비밀번호를 적지 않는다. */
  isAdmin: boolean;
};

const errorClass = "text-sm text-red-600 dark:text-red-400";

export function CommentForm({ action, isAdmin }: Props) {
  const [state, formAction, pending] = useActionState(action, {
    values: { authorName: "", content: "" },
    fieldErrors: {},
  });
  const errors = state.fieldErrors;

  return (
    <form action={formAction} className="mt-6 flex flex-col gap-3">
      {state.message && (
        <p role="alert" className={alertClass}>
          {state.message}
        </p>
      )}

      {!isAdmin && (
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="flex flex-col gap-1">
            <label htmlFor="comment-author" className="text-sm font-medium">
              이름
            </label>
            <input
              id="comment-author"
              name="authorName"
              type="text"
              defaultValue={state.values.authorName}
              required
              maxLength={20}
              autoComplete="nickname"
              aria-invalid={errors.authorName ? true : undefined}
              className={`${fieldClass} text-sm`}
            />
            {errors.authorName && (
              <p className={errorClass}>{errors.authorName}</p>
            )}
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="comment-password" className="text-sm font-medium">
              비밀번호
            </label>
            <input
              id="comment-password"
              name="password"
              type="password"
              required
              minLength={4}
              maxLength={50}
              // 로그인 비밀번호가 아니므로 브라우저가 저장된 비밀번호를 채워 넣지 않게 한다.
              autoComplete="new-password"
              placeholder="댓글을 지울 때 씁니다"
              aria-invalid={errors.password ? true : undefined}
              className={`${fieldClass} text-sm`}
            />
            {errors.password && <p className={errorClass}>{errors.password}</p>}
          </div>
        </div>
      )}

      <div className="flex flex-col gap-1">
        <label htmlFor="comment-content" className="text-sm font-medium">
          {isAdmin ? "글쓴이로 댓글 쓰기" : "내용"}
        </label>
        <textarea
          id="comment-content"
          name="content"
          defaultValue={state.values.content}
          required
          maxLength={1000}
          rows={3}
          aria-invalid={errors.content ? true : undefined}
          className={`${fieldClass} resize-y text-sm leading-6`}
        />
        {errors.content && <p className={errorClass}>{errors.content}</p>}
      </div>

      <div className="flex justify-end">
        <button type="submit" disabled={pending} className={primaryButtonClass}>
          {pending ? "등록 중…" : "댓글 등록"}
        </button>
      </div>
    </form>
  );
}
