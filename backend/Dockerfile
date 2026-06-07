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

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
