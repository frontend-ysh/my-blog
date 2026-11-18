import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { Suspense } from "react";
import { FormSkeleton } from "@/components/form-skeleton";
import { LoginForm } from "@/components/login-form";
import { safeRedirectPath } from "@/lib/redirect";
import { getAdmin } from "@/lib/session";

export const metadata: Metadata = {
  title: "로그인",
  robots: { index: false },
};

export default function LoginPage({ searchParams }: PageProps<"/login">) {
  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-6 text-2xl font-bold tracking-tight">로그인</h1>
      {/* 주소의 ?next= 값과 쿠키는 요청이 와야 알 수 있으므로 Suspense 안에서 읽는다. */}
      <Suspense fallback={<FormSkeleton />}>
        <LoginGate searchParams={searchParams} />
      </Suspense>
    </div>
  );
}

async function LoginGate({
  searchParams,
}: Pick<PageProps<"/login">, "searchParams">) {
  const { next } = await searchParams;
  const nextPath = safeRedirectPath(Array.isArray(next) ? next[0] : next);

  // 이미 로그인했다면 로그인 화면을 보여줄 필요가 없다.
  if (await getAdmin()) {
    redirect(nextPath);
  }
  return <LoginForm next={nextPath} />;
}
