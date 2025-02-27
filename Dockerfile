FROM eclipse-temurin:17-jdk-jammy

# Create a non-root user to run the application
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install network troubleshooting tools
RUN apt-get update && apt-get install -y \
    netcat-openbsd \
    curl \
    dnsutils \
    iputils-ping \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the jar file
COPY ./target/k8sExample-jar-with-dependencies.jar app.jar

# Set ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 4567

# Set environment variables for JVM tuning
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/heap-dump.hprof \
    -Dfile.encoding=UTF-8"

# Set Korean locale and start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -Duser.language=ko \
    -Duser.country=KR \
    -jar app.jar"]
