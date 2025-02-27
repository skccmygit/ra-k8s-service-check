# Build stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build
COPY . .
RUN ./mvnw clean package -DskipTests

# Run stage 
FROM eclipse-temurin:17-jre-jammy

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install network tools
RUN apt-get update && apt-get install -y \
    netcat-openbsd \
    curl \
    dnsutils \
    iputils-ping \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/k8sExample-jar-with-dependencies.jar app.jar
RUN jar tvf app.jar | grep -E "templates/|public/" || echo "Warning: Resources might be missing"

# Set ownership and switch user
RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 4567

# JVM tuning
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/heap-dump.hprof \
    -Dfile.encoding=UTF-8"

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:4567/health || exit 1

# Start app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -Duser.language=ko \
    -Duser.country=KR \
    -jar app.jar"]
