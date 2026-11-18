# 백엔드 이미지. 두 단계로 나눈다: 빌드에는 JDK와 Gradle이 필요하지만 실행에는 JRE와 jar 파일 하나면 된다.
# 빌드 도구와 소스를 최종 이미지에서 빼면 이미지가 작아지고, 공격받을 수 있는 면도 줄어든다.

# ---------- 1단계: 빌드 ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성을 정하는 파일만 먼저 복사해서 라이브러리를 내려받는다.
# Docker는 줄(레이어)마다 결과를 저장해 두고, 그 줄에 쓰인 파일이 바뀌지 않았으면 다시 실행하지 않는다.
# 이렇게 나눠 두면 소스만 고쳤을 때는 라이브러리를 다시 내려받지 않는다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon > /dev/null

COPY src src
# 실행 가능한 jar를 만든다. 테스트는 여기서 돌리지 않는다: 테스트가 Docker(Testcontainers)를
# 쓰는데 이미지 빌드 안에서는 Docker를 쓸 수 없다. 테스트는 CI가 이미지 빌드 전에 따로 돌린다.
RUN ./gradlew bootJar --no-daemon

# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# root가 아닌 전용 계정으로 실행한다. 앱에 취약점이 있어도 컨테이너 안에서 할 수 있는 일이 제한된다.
# 올린 이미지를 저장할 폴더는 그 계정이 쓸 수 있게 미리 만들어 둔다.
RUN useradd --system --uid 1001 app \
    && mkdir /app/uploads \
    && chown app /app/uploads

COPY --from=build /app/build/libs/*.jar app.jar

USER app
ENV UPLOAD_DIR=/app/uploads
EXPOSE 8080

# 컨테이너에 주어진 메모리의 75%까지를 자바 힙으로 쓴다(나머지는 JVM 자체와 스레드 등에 필요하다).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
