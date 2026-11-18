import Link from "next/link";
import { logout } from "@/app/auth/actions";
import { getAdmin } from "@/lib/session";

const linkClass =
  "rounded-md px-3 py-1.5 text-sm text-zinc-600 hover:bg-zinc-100 " +
  "dark:text-zinc-400 dark:hover:bg-zinc-900";

/**
 * 머리말 오른쪽 메뉴. 로그인 여부에 따라 내용이 달라진다.
 * 쿠키를 읽으므로 요청이 와야 그릴 수 있다. 그래서 layout에서 Suspense로 감싸 둔다.
 */
export async function HeaderNav() {
  const admin = await getAdmin();

  if (!admin) {
    return (
      <Link href="/login" className={linkClass}>
        로그인
      </Link>
    );
  }

  return (
    <nav className="flex items-center gap-1">
      <Link
        href="/posts/new"
        className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-900"
      >
        글쓰기
      </Link>
      {/* 로그아웃은 상태를 바꾸는 동작이므로 링크(GET)가 아니라 폼(POST)으로 보낸다. */}
      <form action={logout}>
        <button type="submit" className={linkClass}>
          로그아웃
        </button>
      </form>
    </nav>
  );
}
