# CAMPUSHUB & TEAMMATCH — NỀN TẢNG KẾT NỐI & GHÉP ĐỘI HỌC TẬP, NCKH SINH VIÊN
> **Mini Project Tổng Hợp (Session 13) — Học phần IT209 (Điện toán đám mây & CI/CD)**

---

## 1. Giới thiệu dự án & Điểm mới so với Dự án nền

Hệ thống được phát triển mở rộng từ dự án nền `campus-hub-base`, giải quyết bài toán cấp thiết của sinh viên: **Tìm kiếm đồng đội làm đồ án tốt nghiệp, bài tập lớn, thi Hackathon và Nghiên cứu Khoa học (NCKH)**.

### Tính năng mới phát triển:
1. **Đăng bài tuyển thành viên:** Thiết lập chỉ tiêu số lượng (target members), kỹ năng yêu cầu (skills tags: React, Docker, Spring Boot...) và thông tin liên hệ.
2. **Huy hiệu & Thanh tiến độ (Progress Bar):** Thể hiện trực quan tiến độ tuyển thành viên (ví dụ: `1 / 2 bạn (50%)`) và trạng thái (`ĐANG MỞ TUYỂN (OPEN)`, `ĐÃ ĐỦ NGƯỜI (FULL)`, `ĐÃ ĐÓNG TUYỂN (CLOSED)`).
3. **Quy trình Ứng tuyển hoàn chỉnh:**
   - Sinh viên nộp đơn ứng tuyển kèm lời nhắn và thông tin liên hệ.
   - Kiểm tra ràng buộc chặt chẽ: Chống nộp trùng, ngăn trưởng nhóm tự ứng tuyển, chặn nộp khi nhóm đã đủ người.
4. **Bảng điều khiển dành cho Trưởng nhóm:**
   - Xem danh sách ứng viên gửi đến.
   - Phê duyệt (`ACCEPTED`) hoặc Từ chối (`REJECTED`).
   - Tự động chuyển bài đăng sang trạng thái `FULL` và khóa nhận đơn khi sĩ số được duyệt đạt chỉ tiêu.
5. **Đơn ứng tuyển của tôi:** Theo dõi toàn bộ các nhóm đã gửi đơn và cho phép rút đơn khi còn `PENDING`.

---

## 2. Kiến trúc Đóng gói & Triển khai Container

```text
[ Trình duyệt / Client ]
           │ Port 3000 (Host)
           ▼
┌────────────────────────────────────────────────────────┐
│  campushub-frontend (Vite React + Nginx Alpine)        │
│  - Phục vụ Static SPA                                  │
│  - Reverse Proxy /api/ & /actuator/ sang Backend      │
└──────────────────────────┬─────────────────────────────┘
                           │ Port 8080 (Mạng campushub-network)
                           ▼
┌────────────────────────────────────────────────────────┐
│  campushub-backend (Spring Boot 3.5.7 - JRE 21 Alpine) │
│  - RESTful APIs, Spring Security (Basic Auth)          │
│  - Flyway Migrations V1 & V2                           │
└──────────────────────────┬─────────────────────────────┘
                           │ Port 3306
                           ▼
┌────────────────────────────────────────────────────────┐
│  campushub-mysql (MySQL 8.0 - Healthcheck)             │
│  - Lưu trữ bền vững với Docker Volume: mysql_data      │
└────────────────────────────────────────────────────────┘
```

---

## 3. Hướng dẫn Khởi chạy Hệ thống

### Cách 1: Khởi chạy toàn bộ bằng Docker Compose (Khuyên dùng)
Tại thư mục gốc `campus-hub-base`:
```bash
docker compose up -d --build
```
- **Frontend:** `http://localhost:3000`
- **Backend API:** `http://localhost:8080/api/posts`
- **Healthcheck Actuator:** `http://localhost:8080/actuator/health`

### Cách 2: Khởi chạy cục bộ phục vụ Phát triển (Development)
1. **Cơ sở dữ liệu MySQL:**
   ```bash
   docker compose up -d mysql
   ```
2. **Khởi chạy Backend (Spring Boot):**
   ```bash
   cd backend
   .\gradlew.bat bootRun   # Trên Windows
   ./gradlew bootRun       # Trên Linux/macOS
   ```
3. **Khởi chạy Frontend (Vite React):**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Truy cập: `http://localhost:5173`

---

## 4. Kiểm thử Tự động (Automated Testing)

Chạy bộ kiểm thử tích hợp (gồm 5 kịch bản từ phân quyền đến luồng ghép đội hoàn chỉnh):
```bash
cd backend
.\gradlew.bat test
```
*Kết quả kỳ vọng:* `BUILD SUCCESSFUL`

---

## 5. Tài khoản Dùng thử (Demo Accounts)

| Vai trò | Tên đăng nhập | Mật khẩu | Mục đích kiểm thử |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin` | `admin123` | Quản lý toàn bộ bài đăng, ẩn bài |
| **Trưởng nhóm (Leader)** | `student` | `student123` | Đăng bài tuyển nhóm, duyệt/từ chối ứng viên |
| **Ứng viên (Applicant)** | `haianh` | `student123` | Nộp đơn ứng tuyển, theo dõi trạng thái đơn |

---

## 6. Kịch bản Thuyết trình Bảo vệ (30–35 phút)

1. **Phần 1: Giới thiệu bài toán & Động lực (5 phút):**
   - Vấn đề khó khăn khi sinh viên tìm bạn làm đồ án / NCKH đúng kỹ năng.
   - Giải pháp của nhóm: CampusHub & TeamMatch.
2. **Phần 2: Trình diễn Trực tiếp (Live Demo) (12–15 phút):**
   - Đăng nhập tài khoản `student` $\rightarrow$ Đăng bài tuyển 2 bạn làm đồ án Cloud & DevOps (tags: Docker, Spring Boot).
   - Đăng nhập ẩn danh hoặc tài khoản `haianh` $\rightarrow$ Nộp đơn ứng tuyển.
   - Thử nghiệm validation: Cố tình nộp lại đơn $\rightarrow$ Hệ thống bắt lỗi 409 Conflict.
   - Đăng nhập lại `student` $\rightarrow$ Mở danh sách ứng viên $\rightarrow$ Bấm "Chấp nhận vào nhóm".
   - Quan sát thanh tiến độ tăng lên $1/2$ (50%). Duyệt tiếp bạn thứ 2 $\rightarrow$ Bài tự động chuyển sang `FULL`.
3. **Phần 3: Kiến trúc & Triển khai DevOps (8 phút):**
   - Giải thích Dockerfile multi-stage build cho Frontend & Backend.
   - Giải thích Docker Compose, Nginx Reverse Proxy và Flyway Migration V2.
4. **Phần 4: Hỏi đáp & Phản biện (5–8 phút):**
   - Trả lời câu hỏi của Giảng viên và các nhóm phản biện.
