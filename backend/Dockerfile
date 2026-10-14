# ==========================================
# Stage 1: Build ứng dụng bằng image Gradle tích hợp sẵn JDK 21
# (Tránh phải tải lại file gradle-bin.zip 130MB gây lỗi timeout mạng)
# ==========================================
FROM gradle:jdk21-alpine AS builder

WORKDIR /app

# Copy gradle wrapper và file cấu hình
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Chuẩn hóa ký tự xuống dòng và cấp quyền thực thi
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copy toàn bộ mã nguồn
COPY src ./src

# Đóng gói file JAR bằng Gradle có sẵn trong image (bỏ qua unit test khi build container)
RUN gradle bootJar --no-daemon -x test

# ==========================================
# Stage 2: Môi trường Runtime siêu nhẹ (JRE)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tạo user/group riêng để chạy ứng dụng (Best Practice bảo mật container)
RUN addgroup -S campushub && adduser -S campushub -G campushub

# Copy file jar đã build từ Stage 1
COPY --from=builder /app/build/libs/*.jar app.jar

# Phân quyền cho user campushub
RUN chown campushub:campushub app.jar

# Chạy với user không có đặc quyền root
USER campushub

# Khai báo cổng lắng nghe
EXPOSE 8080

# Thiết lập điểm khởi chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
