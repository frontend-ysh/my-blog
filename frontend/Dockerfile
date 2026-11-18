# 프런트엔드 이미지. 세 단계로 나눈다.
# 최종 이미지에는 Next.js가 "실행에 꼭 필요한 파일만" 골라 모아 준 폴더(standalone)만 들어간다.

# ---------- 1단계: 의존성 설치 ----------
FROM node:22-alpine AS deps
WORKDIR /app
# package.json이 바뀌지 않았으면 이 단계의 결과를 재사용한다(소스만 고쳤을 때 다시 설치하지 않는다).
COPY package.json package-lock.json ./
# npm ci: package-lock.json에 적힌 버전 그대로 설치한다. 누가 언제 빌드해도 같은 결과가 나온다.
RUN npm ci

# ---------- 2단계: 빌드 ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules node_modules
COPY . .
# next.config.ts가 이 값을 보고 실행용 파일을 .next/standalone 폴더에 모아 준다.
ENV BUILD_STANDALONE=1
# 빌드에는 백엔드가 필요 없다. 글 데이터는 실행 중에 요청이 올 때마다 가져온다.
RUN npm run build

# ---------- 3단계: 실행 ----------
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
# 컨테이너 밖에서 들어오는 요청을 받으려면 모든 주소(0.0.0.0)에서 들어야 한다.
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

# standalone 폴더에는 CSS·JS 같은 정적 파일이 들어 있지 않아서 따로 복사한다.
# (public 폴더를 만들게 되면 그것도 여기서 복사해야 한다.)
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static .next/static

# node 이미지에 들어 있는 일반 계정(node)으로 실행한다.
USER node
EXPOSE 3000
CMD ["node", "server.js"]
