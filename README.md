# Blog

Spring Boot(API 서버)와 Next.js(화면)를 분리해서 만드는 블로그.

```
브라우저 ──▶ Next.js (3000) ──▶ Spring Boot (8080) ──▶ PostgreSQL (5432)
              frontend/           backend/              docker-compose.yml
```

| 영역 | 기술 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4.1, Spring Data JPA, Spring Security(JWT), Flyway, Gradle |
| 프런트엔드 | Next.js 16 (App Router), TypeScript, Tailwind CSS, react-markdown |
| DB | PostgreSQL 17 |
| 테스트 | JUnit 5, Testcontainers |
| 배포 | Docker, Docker Compose, GitHub Actions |

## 실행 방법

필요한 것: Java 21, Node.js 20.9 이상, Docker

터미널 세 개에서 차례로 실행한다.

```bash
# 1. DB
docker compose up -d
```

```bash
# 2. 백엔드 → http://localhost:8080/actuator/health
cd backend
./gradlew bootRun
```

```bash
# 3. 프런트엔드 → http://localhost:3000
cd frontend
npm install
npm run dev
```

`./gradlew bootRun`은 개발용 설정(`application-local.yml`)으로 실행되고, 관리자 계정이 자동으로 만들어진다.

| 아이디 | 비밀번호 |
|---|---|
| `admin` | `admin1234` |

http://localhost:3000/login 에서 로그인하면 글쓰기·수정·삭제 버튼이 나타난다.

## Docker로 전체 띄우기

DB, 백엔드, 프런트엔드를 이미지로 만들어 한 번에 띄운다. Docker만 있으면 되고 Java와 Node.js는 필요 없다.

```bash
cp .env.example .env
```

`.env`를 열어 `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`를 채운 뒤 실행한다.

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

http://localhost:3000 으로 접속한다.

| 서비스 | 이미지 | 밖으로 여는 포트 | 데이터 |
|---|---|---|---|
| `frontend` | `frontend/Dockerfile` | 3000 | |
| `backend` | `backend/Dockerfile` | 없음 | 볼륨 `uploads` (올린 이미지) |
| `db` | `postgres:17-alpine` | 없음 | 볼륨 `db-data` |

- 백엔드와 DB는 내부 네트워크에서만 접속된다. 밖에서는 프런트엔드만 보인다.
- 컨테이너를 지우고 다시 만들어도 글, 댓글, 올린 이미지는 볼륨에 남는다. `down`에 `--volumes`를 붙이면 데이터까지 지워진다.
- 코드를 고친 뒤에는 같은 `up -d --build` 명령을 다시 실행하면 바뀐 이미지로 교체된다.
- **실제 서버에서는 HTTPS가 필요하다.** 로그인 쿠키는 HTTPS(또는 localhost)에서만 전송되도록 되어 있어서, `http://서버주소:3000`으로는 로그인이 되지 않는다. 앞단에 HTTPS를 처리하는 프록시(Caddy, Nginx, 클라우드의 로드 밸런서 등)를 두고 3000번 포트로 넘겨야 한다.

## CI

`.github/workflows/ci.yml`. `main`에 올리거나 풀 리퀘스트를 열면 GitHub Actions가 다음을 자동으로 돌린다.

| 작업 | 하는 일 |
|---|---|
| 백엔드 테스트 | `./gradlew test` |
| 프런트엔드 린트·빌드 | `npm ci`, `npm run lint`, `npm run build` |
| Docker로 전체 띄워 보기 | 위 둘이 통과하면, 두 이미지를 빌드해 `docker-compose.prod.yml`로 띄우고 첫 화면이 DB까지 연결되어 나오는지 확인 |

## 운영 환경 설정

운영에서는 개발용 설정을 쓰지 않고 환경변수로 값을 넣는다. Docker로 띄울 때는 `.env` 파일의 값이 아래 환경변수로 전달된다.

| 환경변수 | 대상 | 설명 |
|---|---|---|
| `JWT_SECRET` | 백엔드 | 토큰 서명 키. 필수, 32자 이상. 없으면 서버가 뜨지 않는다. |
| `JWT_EXPIRATION` | 백엔드 | 토큰 유효 기간. 기본 `12h` |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | 백엔드 | 처음 뜰 때 만들 관리자 계정. 이미 있으면 건드리지 않는다. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 백엔드 | PostgreSQL 접속 정보 |
| `UPLOAD_DIR` | 백엔드 | 올린 이미지를 저장할 폴더. 기본 `uploads` (서버를 실행한 위치 기준) |
| `API_BASE_URL` | 프런트엔드 | 백엔드 주소. 기본 `http://localhost:8080` |

## 테스트

Docker가 실행 중이어야 한다. 테스트용 PostgreSQL 컨테이너가 자동으로 떴다가 사라진다.

```bash
cd backend
./gradlew test
```

## 화면

| 주소 | 설명 |
|---|---|
| `/` | 글 목록. `/?q=검색어&tag=spring&page=2`처럼 검색어, 태그, 페이지를 지정할 수 있다 |
| `/posts/{id}` | 글 상세 (본문은 마크다운으로 표시)와 댓글 |
| `/posts/new` | 글 작성 (관리자) |
| `/posts/{id}/edit` | 글 수정 (관리자) |
| `/login` | 로그인 |
| `/images/{파일}` | 올린 이미지. Next.js가 백엔드에서 받아 전달한다 |

화면은 Next.js 서버에서 백엔드 API를 호출해 HTML로 만들어 보낸다. 작성·수정·삭제는 서버 액션(`frontend/src/app/posts/actions.ts`)이 처리한다.

## 로그인 구조

```
브라우저 ──(httpOnly 쿠키)──▶ Next.js ──(Authorization: Bearer 토큰)──▶ Spring Boot
```

1. 로그인하면 백엔드가 JWT를 발급하고, Next.js가 그것을 자바스크립트로 읽을 수 없는 쿠키에 저장한다.
2. 이후 요청마다 Next.js가 쿠키에서 토큰을 꺼내 백엔드로 전달한다.
3. 토큰이 유효한지, 관리자인지는 언제나 백엔드가 판단한다. 화면에서 버튼을 숨기는 것은 편의일 뿐이다.

## API

| 메서드 | 경로 | 설명 | 성공 | 실패 |
|---|---|---|---|---|
| POST | `/api/auth/login` | 로그인, 토큰 발급 | 200 | 400, 401 |
| GET | `/api/auth/me` | 토큰의 주인 확인 (로그인 필요) | 200 | 401 |
| GET | `/api/posts` | 글 목록 (최신순, 본문 제외, 페이지 단위) | 200 | 400 |
| GET | `/api/tags` | 태그 목록과 태그별 글 수 | 200 | |
| GET | `/api/posts/{id}` | 글 상세 | 200 | 404 |
| POST | `/api/posts` | 글 작성 (관리자) | 201 + `Location` 헤더 | 400, 401, 403 |
| PUT | `/api/posts/{id}` | 글 수정 (관리자) | 200 | 400, 401, 403, 404 |
| DELETE | `/api/posts/{id}` | 글 삭제 (관리자). 댓글도 함께 지워진다 | 204 | 401, 403, 404 |
| GET | `/api/posts/{postId}/comments` | 댓글 목록 (쓴 순서) | 200 | 404 |
| POST | `/api/posts/{postId}/comments` | 댓글 작성 | 201 | 400, 404 |
| DELETE | `/api/comments/{id}` | 댓글 삭제 (비밀번호 또는 관리자) | 204 | 403, 404 |
| POST | `/api/images` | 이미지 업로드 (관리자) | 201 | 400, 401, 403, 413 |
| GET | `/images/{파일}` | 올린 이미지 | 200 | 404 |

401은 로그인하지 않았거나 토큰이 만료·위조된 경우, 403은 로그인했지만 관리자가 아닌 경우다.

요청 본문(POST, PUT `/api/posts`):

| 항목 | 규칙 |
|---|---|
| `title` | 필수, 200자 이하 |
| `content` | 필수. 마크다운 |
| `tags` | 선택. 문자열 배열, 10개 이하, 각 30자 이하. 소문자로 통일해 저장한다 |

목록 조회(`GET /api/posts`)의 파라미터:

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `page` | 1 | 페이지 번호 (1부터) |
| `size` | 10 | 한 페이지의 글 수 (1~50) |
| `tag` | 없음 | 이 태그가 붙은 글만 |
| `q` | 없음 | 제목이나 본문에 이 말이 든 글만 (대소문자 무시, 100자 이하) |

```json
{
  "content": [{ "id": 3, "title": "글 제목", "tags": ["jpa", "spring"], "createdAt": "2026-11-17T06:01:04.704982Z" }],
  "page": 1,
  "size": 10,
  "totalElements": 23,
  "totalPages": 3
}
```

```bash
# 로그인해서 토큰 받기
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin1234"}'
```

```bash
# 받은 accessToken을 Authorization 헤더에 실어 글 쓰기
curl -i -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 여기에_토큰" \
  -d '{"title":"첫 글","content":"**안녕하세요**","tags":["spring","jpa"]}'
```

오류 응답은 Problem Details(RFC 9457) 형식이고, 입력 검증에 실패하면 `errors`에 필드별 사유가 담긴다.

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값이 올바르지 않습니다.",
  "instance": "/api/posts",
  "errors": [{ "field": "title", "message": "제목은 필수입니다." }]
}
```

댓글은 회원가입 없이 쓴다.

| 누가 | 작성 요청 본문 | 삭제 |
|---|---|---|
| 방문자 | `authorName`(20자 이하), `password`(4~50자), `content`(1000자 이하) | 본문에 `{"password": "..."}`를 실어 보낸다 |
| 관리자 (토큰 필요) | `content`만. 이름은 로그인 아이디가 되고 `byAdmin`이 `true`가 된다 | 비밀번호 없이 어떤 댓글이든 |

이미지는 `multipart/form-data`의 `file` 항목으로 올린다. PNG, JPEG, GIF, WebP만 받고 5MB까지다. 형식은 파일 이름이 아니라 내용으로 판별한다.

```bash
curl -X POST http://localhost:8080/api/images \
  -H "Authorization: Bearer 여기에_토큰" \
  -F "file=@사진.png"
```

응답의 `url`(예: `/images/3f2b….png`)을 본문에 `![설명](/images/3f2b….png)`로 넣는다. 글쓰기 화면의 "이미지 첨부" 버튼이 이 과정을 대신 해 준다.

## 진행 단계

- [x] 0단계: 프로젝트 뼈대 (백엔드, 프런트엔드, DB 연결)
- [x] 1단계: 글 CRUD API
- [x] 2단계: 글 목록·상세·작성·수정 화면
- [x] 3단계: 관리자 로그인 (Spring Security, JWT)
- [x] 4단계: 태그, 페이지네이션, 마크다운
- [x] 5단계: 댓글, 이미지 업로드, 검색
- [x] 6단계-1: Docker 이미지, 전체 구성(docker-compose.prod.yml), CI 설정
- [ ] 6단계-2: 실제 서버에 배포 (배포할 곳을 정한 뒤)
