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
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/heap-dump.hprof \
    -Dfile.encoding=UTF-8"

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:4567/checkutil/health || exit 1

# Tomcat Native 라이브러리 버전 설정
ENV TOMCAT_NATIVE_VERSION=2.0.7
ENV LD_LIBRARY_PATH ${LD_LIBRARY_PATH}:/usr/lib

# Tomcat Native 라이브러리 다운로드 및 설치
RUN cd /tmp \
    && wget https://archive.apache.org/dist/tomcat/tomcat-connectors/native/${TOMCAT_NATIVE_VERSION}/source/tomcat-native-${TOMCAT_NATIVE_VERSION}-src.tar.gz \
    && tar xzf tomcat-native-${TOMCAT_NATIVE_VERSION}-src.tar.gz \
    && cd tomcat-native-${TOMCAT_NATIVE_VERSION}-src/native \
    && ./configure --with-java-home=${JAVA_HOME} \
                  --with-apr=/usr/bin/apr-1-config \
                  --prefix=/usr \
                  --libdir=/usr/lib \
    && make \
    && make install \
    && cd / \
    && rm -rf /tmp/tomcat-native-${TOMCAT_NATIVE_VERSION}-src \
    && rm /tmp/tomcat-native-${TOMCAT_NATIVE_VERSION}-src.tar.gz

# Start app with debug logging
ENTRYPOINT ["java", "-jar", "/app.jar"]
