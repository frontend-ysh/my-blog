import type { Metadata } from "next";
import { Suspense } from "react";
import { FormSkeleton } from "@/components/form-skeleton";
import { PostForm } from "@/components/post-form";
import { getPost } from "@/lib/posts";
import { requireAdmin } from "@/lib/session";
import { updatePost } from "../../actions";

export const metadata: Metadata = {
  title: "글 수정",
  robots: { index: false },
};

export default function EditPostPage({
  params,
}: PageProps<"/posts/[id]/edit">) {
  return (
    <>
      <h1 className="mb-6 text-2xl font-bold tracking-tight">글 수정</h1>
      <Suspense fallback={<FormSkeleton tall />}>
        <EditForm params={params} />
      </Suspense>
    </>
  );
}

/** 기존 글을 불러와 폼에 채운다. 작성 화면과 같은 PostForm을 쓰고, 넘기는 액션만 다르다. */
async function EditForm({
  params,
}: Pick<PageProps<"/posts/[id]/edit">, "params">) {
  const { id } = await params;
  // 글이 있는지부터 확인한다(없으면 404). 그다음 관리자인지 확인한다.
  const post = await getPost(id);
  await requireAdmin(`/posts/${post.id}/edit`);

  return (
    <PostForm
      // Next.js는 떠난 화면을 지우지 않고 숨겨 두었다가, 다시 오면 그대로 꺼내 보여준다
      // (쓰던 내용을 보존하기 위해서다). 그런데 저장하고 나면 그 "쓰던 내용"은 수정 전 값이라,
      // 그대로 다시 보여주면 옛 내용으로 덮어쓰게 된다. key가 바뀌면 React는 컴포넌트를
      // 버리고 새로 만들므로, 글이 수정될 때마다 폼이 최신 값으로 다시 만들어진다.
      // (태그만 바꾸면 updatedAt은 그대로라서, 태그도 key에 넣는다.)
      key={`${post.updatedAt}|${post.tags.join(",")}`}
      action={updatePost.bind(null, post.id)}
      initialValues={{
        title: post.title,
        content: post.content,
        tags: post.tags.join(", "),
      }}
      submitLabel="수정"
      cancelHref={`/posts/${post.id}`}
    />
  );
}
