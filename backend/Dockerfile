# ============================================================
# Finans Portalı – Backend Dockerfile
# Multi-stage build: Maven build → JRE runtime
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Önce pom.xml kopyala – bağımlılık cache'i için
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kaynak kodu kopyala ve paketle
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Finans Portalı Ekibi"
LABEL description="Finance Portal Backend – Spring Boot"

WORKDIR /app

# Güvenlik: root olmayan kullanıcı
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Jar dosyasını kopyala
COPY --from=build /app/target/*.jar app.jar

# Log dizini
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

# JVM ayarları
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
