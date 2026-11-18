import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
import { Suspense } from "react";
import { HeaderNav } from "@/components/header-nav";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  // 각 페이지가 title을 정하면 "글 제목 | Blog" 형태가 되고, 정하지 않으면 "Blog"가 된다.
  title: { default: "Blog", template: "%s | Blog" },
  description: "개발하며 배운 것을 기록하는 블로그",
};

/** 모든 페이지를 감싸는 공통 틀. 머리말은 여기 한 번만 두고, 페이지 내용이 children으로 들어온다. */
export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="ko"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col font-sans">
        <header className="border-b border-zinc-200 dark:border-zinc-800">
          <div className="mx-auto flex w-full max-w-2xl items-center justify-between px-6 py-4">
            <Link href="/" className="text-lg font-bold tracking-tight">
              Blog
            </Link>
            {/* 메뉴는 로그인 여부를 확인해야 그릴 수 있다. 확인하는 동안 높이만 잡아 둔다. */}
            <Suspense fallback={<div className="h-8" />}>
              <HeaderNav />
            </Suspense>
          </div>
        </header>
        <main className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
          {children}
        </main>
      </body>
    </html>
  );
}
