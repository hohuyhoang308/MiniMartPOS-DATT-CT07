# ===== Build stage: biên dịch & đóng gói fat-jar bằng Maven =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Tải dependency trước (tận dụng cache layer: chỉ chạy lại khi pom.xml đổi)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Biên dịch & đóng gói (bỏ test ở bước build ảnh — test chạy riêng qua `mvn test`/CI)
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ===== Runtime stage: chỉ JRE + jar cho ảnh gọn =====
FROM eclipse-temurin:17-jre
WORKDIR /app

# Múi giờ VN cho log/đối soát đúng giờ
ENV TZ=Asia/Ho_Chi_Minh

# curl cho Docker HEALTHCHECK (gọi /actuator/health)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Liveness/readiness: container UNHEALTHY nếu app không trả health "UP" → Docker/K8s tự restart/không route.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
