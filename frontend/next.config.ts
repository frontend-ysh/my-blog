import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  // standalone: 빌드할 때 실행에 꼭 필요한 파일만 .next/standalone 폴더에 모아 준다.
  // Docker 이미지에는 이 폴더만 넣으면 되어서(node_modules 전체가 필요 없다) 이미지가 작아진다.
  // Dockerfile이 BUILD_STANDALONE을 켜고 빌드한다. 평소(npm run build, npm run start)에는 꺼져 있다.
  // 이 설정이 켜져 있으면 npm run start가 동작하지 않기 때문이다.
  output: process.env.BUILD_STANDALONE ? "standalone" : undefined,
  cacheComponents: true,
  partialPrefetching: true,
  experimental: {
    serverActions: {
      // 서버 액션이 받는 요청의 크기 한도. 기본값은 1MB라서 이미지를 올릴 수 없다.
      // 이미지는 5MB까지 받으므로(백엔드 ImageService), 폼 데이터의 부가 정보만큼 여유를 둔다.
      bodySizeLimit: "6mb",
    },
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
