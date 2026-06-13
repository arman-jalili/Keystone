# =============================================================================
# Keystone Server — Dockerfile
# =============================================================================
# Multi-stage build: Maven compile → distroless runtime
#
# Build:    docker build -t keystone-server ./backend
# Run:      docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev keystone-server
# =============================================================================

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy parent POM
COPY pom.xml ./
COPY backend/pom.xml ./backend/

# Download dependencies (layer caching)
RUN mvn dependency:go-offline -pl backend -am -q -B

# Copy source
COPY backend/src ./backend/src

# Build the JAR (skip tests for speed; tests run in CI)
RUN mvn package -pl backend -am -DskipTests -q -B

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="Keystone Server" \
      org.opencontainers.image.description="OpenAPI specification governance server" \
      org.opencontainers.image.version="0.1.0"

# Create non-root user
RUN addgroup -S keystone && adduser -S keystone -G keystone

WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /build/backend/target/keystone-server-*.jar app.jar

# Runtime user
USER keystone

# Expose HTTP and Actuator ports
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Default profile = dev (H2 in-memory). Override with -e SPRING_PROFILES_ACTIVE=prod
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java", "-jar", "app.jar"]
