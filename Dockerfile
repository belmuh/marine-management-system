# ═══════════════════════════════════════════════════════════════
# Build aşaması — Maven wrapper projedeki .mvn/wrapper/maven-wrapper.properties
# dosyasından hangi Maven'ı indireceğini bilir. Bu aşama son imaja GİRMEZ.
# ═══════════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Önce sadece build tanımlarını kopyala → bağımlılık katmanı cache'lenir,
# kod değişince Maven her şeyi yeniden indirmez.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

# ═══════════════════════════════════════════════════════════════
# Çalışma aşaması — sadece JRE + jar. Maven, kaynak kod, test yok.
# ═══════════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jre
WORKDIR /app

# Healthcheck için curl (temurin-jre imajında yok) — root iken kurulmalı
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Root olmayan kullanıcı (güvenlik)
RUN useradd --system --uid 1001 appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

# Çalışma zamanı yapılandırması env ile gelir (bkz. .env.example):
# DATABASE_URL/USERNAME/PASSWORD, JWT_SECRET, CORS_ALLOWED_ORIGINS, R2_*, MAIL_* ...
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
