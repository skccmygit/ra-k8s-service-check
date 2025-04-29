# 기본 이미지 - JDK 17 사용
FROM eclipse-temurin:17-jdk

# Create non-root user (Alpine 방식으로 수정)
RUN addgroup -S appuser && adduser -S -G appuser appuser

# Install network tools (Alpine 패키지 관리자 사용)
RUN apk update && apk add --no-cache \
    netcat-openbsd \
    curl \
    bind-tools \
    iputils \
    net-tools

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사
COPY target/*.jar app.jar

# Set ownership and switch user
RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 4567

# 애플리케이션 실행
ENTRYPOINT ["java", "--add-opens", "java.base/java.io=ALL-UNNAMED", "-jar", "app.jar"]
