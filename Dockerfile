# 기본 이미지 - JDK 17 사용
FROM eclipse-temurin:17-jdk

# Create non-root user 
#RUN addgroup -S appuser && adduser -S -G appuser appuser
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install network tools 
RUN apt-get update && apt-get install -y \
    netcat \
    curl \
    dnsutils \
    iputils-ping \
    net-tools \
 && rm -rf /var/lib/apt/lists/*


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
