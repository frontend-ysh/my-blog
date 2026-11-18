import Link from "next/link";
import { listHref } from "@/lib/list-params";

type Props = {
  name: string;
  /** 붙은 글의 수. 주면 이름 옆에 보여준다. */
  count?: number;
  /** 지금 이 태그로 걸러 보고 있는지. */
  active?: boolean;
};

/** 누르면 그 태그가 붙은 글 목록으로 가는 꼬리표. */
export function TagLink({ name, count, active = false }: Props) {
  return (
    <Link
      href={listHref({ tag: name })}
      aria-current={active ? "true" : undefined}
      className={
        "rounded-full px-2.5 py-0.5 text-xs " +
        (active
          ? "bg-foreground text-background"
          : "bg-zinc-100 text-zinc-700 hover:bg-zinc-200 dark:bg-zinc-800 dark:text-zinc-300 dark:hover:bg-zinc-700")
      }
    >
      #{name}
      {count !== undefined && <span className="ml-1 opacity-60">{count}</span>}
    </Link>
  );
}
