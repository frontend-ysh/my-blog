import ReactMarkdown, { defaultUrlTransform } from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import remarkGfm from "remark-gfm";

/**
 * 마크다운 문자열을 HTML로 바꿔 보여준다.
 *
 * - 서버 컴포넌트라서 변환은 서버에서 끝난다. 브라우저에는 결과 HTML만 가고, 변환 코드는 보내지 않는다.
 * - 본문에 섞인 HTML 태그(<script> 등)는 태그로 해석하지 않고 글자 그대로 보여준다.
 *   javascript: 같은 위험한 링크 주소도 지운다. 둘 다 react-markdown의 기본 동작이다.
 *
 * remarkGfm: 표, 취소선, 체크박스 목록 같은 GitHub식 문법을 더한다.
 * rehypeHighlight: ```js 처럼 언어를 적은 코드 블록에 색을 입힌다.
 */
export function Markdown({ children }: { children: string }) {
  return (
    // prose: 제목·문단·목록·표 등에 읽기 좋은 기본 모양을 입히는 Tailwind 타이포그래피 플러그인
    <div className="prose prose-zinc max-w-none wrap-anywhere dark:prose-invert prose-headings:tracking-tight">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight]}
        // 기본 동작은 위험한 주소(javascript: 등)를 빈 문자열로 바꾼다. 그런데 <img src="">는
        // 브라우저가 지금 보고 있는 페이지를 이미지로 한 번 더 요청하게 만든다.
        // 빈 문자열 대신 속성 자체를 없앤다.
        urlTransform={(url) => defaultUrlTransform(url) || undefined}
        components={{
          // 글 제목이 이미 h1이다. 본문의 "# 제목"은 한 단계 낮춰 페이지에 h1이 하나만 있게 한다.
          h1: "h2",
          // 칸이 많은 표는 좁은 화면에서 넘친다. 표만 가로로 스크롤되게 감싼다.
          table: ({ children }) => (
            <div className="overflow-x-auto">
              <table>{children}</table>
            </div>
          ),
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  );
}
