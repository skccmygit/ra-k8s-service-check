# Run stage only
FROM eclipse-temurin:17-jdk

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install network tools
RUN apt-get update && apt-get install -y \
    netcat-openbsd \
    curl \
    dnsutils \
    iputils-ping \
    net-tools \
    libapr1 \
    libapr1-dev \
    libssl-dev \
    gcc \
    make \
    libtool \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy pre-built JAR
COPY target/*.jar app.jar
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
    -Xdebug \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/heap-dump.hprof \
    -Dfile.encoding=UTF-8"



# Start app with debug logging
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --debug"]
