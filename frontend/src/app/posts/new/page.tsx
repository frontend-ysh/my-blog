import type { Metadata } from "next";
import { Suspense } from "react";
import { FormSkeleton } from "@/components/form-skeleton";
import { PostForm } from "@/components/post-form";
import { requireAdmin } from "@/lib/session";
import { createPost } from "../actions";

export const metadata: Metadata = {
  title: "새 글",
  // 글쓰기 화면은 검색 결과에 나올 필요가 없다.
  robots: { index: false },
};

export default function NewPostPage() {
  return (
    <>
      <h1 className="mb-6 text-2xl font-bold tracking-tight">새 글</h1>
      <Suspense fallback={<FormSkeleton tall />}>
        <NewPostForm />
      </Suspense>
    </>
  );
}

async function NewPostForm() {
  // 관리자가 아니면 여기서 로그인 화면으로 보낸다.
  // 화면을 막는 것은 편의이고, 실제 저장 권한은 백엔드가 다시 검사한다.
  await requireAdmin("/posts/new");
  return <PostForm action={createPost} submitLabel="등록" cancelHref="/" />;
}
