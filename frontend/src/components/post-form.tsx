"use client";

// "use client"가 붙은 컴포넌트는 브라우저에서도 실행된다. 폼의 상태(오류 메시지,
// 저장 중 표시)를 다루려면 브라우저에서 동작하는 훅(useActionState)이 필요하다.

import Link from "next/link";
import { useActionState } from "react";
import type { PostFormState, PostFormValues } from "@/lib/post-form-state";
import { alertClass, fieldClass, primaryButtonClass } from "./form-styles";
import { ImageUploadButton } from "./image-upload-button";

type Props = {
  /** 제출하면 실행할 서버 액션. 작성 화면과 수정 화면이 서로 다른 액션을 넘긴다. */
  action: (state: PostFormState, formData: FormData) => Promise<PostFormState>;
  initialValues?: PostFormValues;
  submitLabel: string;
  cancelHref: string;
};

const EMPTY_VALUES: PostFormValues = { title: "", content: "", tags: "" };

export function PostForm({
  action,
  initialValues = EMPTY_VALUES,
  submitLabel,
  cancelHref,
}: Props) {
  // state: 서버 액션이 마지막으로 돌려준 값 (처음에는 두 번째 인자)
  // formAction: <form action>에 넘길 함수
  // pending: 제출하고 응답을 기다리는 동안 true
  const [state, formAction, pending] = useActionState(action, {
    values: initialValues,
    fieldErrors: {},
  });

  return (
    <form action={formAction} className="flex flex-col gap-5">
      {state.message && (
        <p role="alert" className={alertClass}>
          {state.message}
          {state.loginRequired && (
            <>
              {" "}
              {/* 새 탭에서 열어야 이 화면(쓰던 글)이 그대로 남는다. */}
              <Link
                href="/login"
                target="_blank"
                className="font-medium underline underline-offset-4"
              >
                새 탭에서 로그인
              </Link>
            </>
          )}
        </p>
      )}

      <div className="flex flex-col gap-1.5">
        <label htmlFor="title" className="text-sm font-medium">
          제목
        </label>
        <input
          id="title"
          name="title"
          type="text"
          defaultValue={state.values.title}
          required
          maxLength={200}
          placeholder="제목을 입력하세요"
          aria-invalid={state.fieldErrors.title ? true : undefined}
          aria-describedby={state.fieldErrors.title ? "title-error" : undefined}
          className={fieldClass}
        />
        {state.fieldErrors.title && (
          <p id="title-error" className="text-sm text-red-600 dark:text-red-400">
            {state.fieldErrors.title}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="tags" className="text-sm font-medium">
          태그
        </label>
        <input
          id="tags"
          name="tags"
          type="text"
          defaultValue={state.values.tags}
          placeholder="쉼표로 구분 (예: spring, jpa)"
          autoCapitalize="none"
          aria-invalid={state.fieldErrors.tags ? true : undefined}
          aria-describedby={state.fieldErrors.tags ? "tags-error" : "tags-hint"}
          className={fieldClass}
        />
        {state.fieldErrors.tags ? (
          <p id="tags-error" className="text-sm text-red-600 dark:text-red-400">
            {state.fieldErrors.tags}
          </p>
        ) : (
          <p id="tags-hint" className="text-xs text-zinc-500">
            10개까지 붙일 수 있고, 대소문자는 구분하지 않습니다.
          </p>
        )}
      </div>

      <div className="flex flex-col gap-1.5">
        <div className="flex items-end justify-between gap-2">
          <label htmlFor="content" className="text-sm font-medium">
            내용
          </label>
          <ImageUploadButton targetId="content" />
        </div>
        <textarea
          id="content"
          name="content"
          defaultValue={state.values.content}
          required
          rows={16}
          placeholder="마크다운으로 작성할 수 있습니다"
          aria-invalid={state.fieldErrors.content ? true : undefined}
          aria-describedby={
            state.fieldErrors.content ? "content-error" : undefined
          }
          className={`${fieldClass} resize-y font-mono text-sm leading-7`}
        />
        {state.fieldErrors.content && (
          <p
            id="content-error"
            className="text-sm text-red-600 dark:text-red-400"
          >
            {state.fieldErrors.content}
          </p>
        )}
      </div>

      <div className="flex items-center justify-end gap-2">
        <Link
          href={cancelHref}
          className="rounded-md px-4 py-2 text-sm text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-900"
        >
          취소
        </Link>
        <button
          type="submit"
          disabled={pending}
          className={primaryButtonClass}
        >
          {pending ? "저장 중…" : submitLabel}
        </button>
      </div>
    </form>
  );
}
