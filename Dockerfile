# 1단계: 기본 이미지 - Ubuntu + OpenJDK 17
FROM openjdk:17-slim

# 네트워크 도구 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    netcat \
    curl \
    dnsutils \
    iputils-ping \
    net-tools \
 && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사
COPY target/*.jar app.jar

# 소유권 설정 및 user 변경
RUN chown appuser:appuser app.jar
USER appuser

# 포트 노출
EXPOSE 4567

# 애플리케이션 실행
ENTRYPOINT ["java", "--add-opens=java.base/java.io=ALL-UNNAMED", "-jar", "app.jar"]



