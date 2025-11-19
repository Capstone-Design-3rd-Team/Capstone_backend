# Build stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /build

# Copy Gradle files first for better caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build JAR (skip tests - already done in CI)
RUN gradle bootJar --no-daemon -x test

# ==========================================
# Runtime stage
# ==========================================
# [중요] -jammy 태그 사용: 최신 리눅스(Noble) 대신 안정적인 Ubuntu 22.04(Jammy) 사용
# 이렇게 해야 libasound2 같은 패키지 설치 에러가 안 납니다.
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 1. Playwright 의존성 및 유틸리티 설치
# 'unzip'을 추가했습니다 (jar 파일 압축 해제용)
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    gnupg \
    unzip \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libwayland-client0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxkbcommon0 \
    libxrandr2 \
    xdg-utils \
    && rm -rf /var/lib/apt/lists/*

# 2. Copy JAR from build stage
COPY --from=builder /build/build/libs/*.jar app.jar

# 3. Create a non-root user for security
RUN useradd -m -u 1001 appuser && chown -R appuser:appuser /app

# 4. 사용자 전환 (이후 명령어는 appuser 권한으로 실행)
USER appuser

# [핵심 수정] 5. Playwright 브라우저 미리 설치 (Build Time)
# 앱을 실행하지 않고, 라이브러리만 풀어서 브라우저 다운로드 CLI를 강제 실행합니다.
# 이렇게 하면 DB 연결 없이 브라우저만 다운로드되므로 빌드 시 에러가 안 납니다.
RUN unzip -q app.jar -d /tmp/app && \
    java -cp "/tmp/app/BOOT-INF/lib/*" com.microsoft.playwright.CLI install && \
    rm -rf /tmp/app

# Expose application port
EXPOSE 8080

# Health check
# (브라우저가 이미 설치되어 있으므로 서버가 빨리 뜹니다)
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]