import { apiFetch } from "@/lib/api";

// 올린 이미지를 브라우저에 전달하는 길목.
//
//   브라우저 ── GET /images/abc.png ──▶ Next.js(이 파일) ── GET /images/abc.png ──▶ Spring Boot
//
// 브라우저는 백엔드와 직접 통신하지 않으므로, 글 본문의 이미지 주소(/images/…)도 Next.js로 온다.
// 여기서 백엔드에 같은 파일을 요청해 그대로 흘려보낸다.
// page.tsx가 화면을 돌려주는 파일이라면, route.ts는 화면이 아닌 응답(파일, JSON 등)을 돌려주는 파일이다.

/** 백엔드가 만드는 파일 이름의 모양: UUID + 확장자. 이 모양이 아니면 백엔드에 묻지도 않는다. */
const FILE_NAME = /^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\.(png|jpg|gif|webp)$/;

/** 백엔드의 응답에서 그대로 넘겨줄 헤더. */
const PASS_THROUGH_HEADERS = [
  "content-type",
  "content-length",
  "cache-control",
  "last-modified",
];

export async function GET(
  _request: Request,
  { params }: RouteContext<"/images/[name]">,
) {
  const { name } = await params;
  if (!FILE_NAME.test(name)) {
    return new Response(null, { status: 404 });
  }

  let upstream: Response;
  try {
    // 큰 이미지는 받는 데 시간이 걸릴 수 있어 제한 시간을 넉넉히 준다.
    upstream = await apiFetch(`/images/${name}`, { timeoutMs: 30_000 });
  } catch {
    return new Response(null, { status: 502 });
  }
  if (!upstream.ok) {
    return new Response(null, { status: upstream.status === 404 ? 404 : 502 });
  }

  const headers = new Headers();
  for (const header of PASS_THROUGH_HEADERS) {
    const value = upstream.headers.get(header);
    if (value) {
      headers.set(header, value);
    }
  }
  // 브라우저가 내용을 보고 파일 종류를 멋대로 추측하지 못하게 한다.
  headers.set("X-Content-Type-Options", "nosniff");

  // 본문을 다 받아 두지 않고, 받는 대로 브라우저에 흘려보낸다.
  return new Response(upstream.body, { status: 200, headers });
}
