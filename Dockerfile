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

# Set Korean locale through Java system properties
ENTRYPOINT ["java", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", \
    "-Duser.language=ko", \
    "-Duser.country=KR", \
    "-jar", \
    "app.jar" \
]
