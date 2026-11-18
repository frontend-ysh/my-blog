"use client";

import { useActionState } from "react";
import { login } from "@/app/auth/actions";
import { alertClass, fieldClass, primaryButtonClass } from "./form-styles";

/** @param next 로그인에 성공하면 이동할 주소. 서버에서 이미 검증한 값이 들어온다. */
export function LoginForm({ next }: { next: string }) {
  const [state, formAction, pending] = useActionState(login, { username: "" });

  return (
    <form action={formAction} className="flex flex-col gap-5">
      {state.message && (
        <p role="alert" className={alertClass}>
          {state.message}
        </p>
      )}

      <input type="hidden" name="next" value={next} />

      <div className="flex flex-col gap-1.5">
        <label htmlFor="username" className="text-sm font-medium">
          아이디
        </label>
        <input
          id="username"
          name="username"
          type="text"
          defaultValue={state.username}
          required
          autoComplete="username"
          autoCapitalize="none"
          className={fieldClass}
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="password" className="text-sm font-medium">
          비밀번호
        </label>
        <input
          id="password"
          name="password"
          type="password"
          required
          autoComplete="current-password"
          className={fieldClass}
        />
      </div>

      <button type="submit" disabled={pending} className={primaryButtonClass}>
        {pending ? "로그인 중…" : "로그인"}
      </button>
    </form>
  );
}
