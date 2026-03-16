<!-- instructions/architecture.md -->

# 7. Kiến trúc & Luồng Dữ liệu (Architecture & Data Flow)

Tài liệu này định nghĩa cấu trúc hệ thống và cách phối hợp giữa các thành phần để đảm bảo hiệu năng và tính nhất quán dữ liệu.

---

## 1. Sơ đồ Kiến trúc Hệ thống (System Architecture)

Dự án tuân thủ mô hình **Modular Monolith** với kiến trúc phân tầng, cho phép dễ dàng bảo trì và mở rộng sang Mobile App.

### Các lớp thành phần:

- **Presentation Layer (React TS):** Xử lý UI/UX và trạng thái ứng dụng phía Client.
- **API Gateway (Nginx):** Đóng vai trò Reverse Proxy, quản lý SSL và điều phối traffic.
- **Service Layer (Spring Boot):** Thực thi logic nghiệp vụ, điều phối giữa các Database và Storage.
- **Data Layer:**
  - **PostgreSQL:** Lưu trữ dữ liệu giao dịch cốt lõi (ACID).
  - **MongoDB:** Lưu trữ dữ liệu Social, Logs, Interactions và Chat/Messaging (High-write).
  - **Redis:** Lưu trữ Cache và tính toán Leaderboard (Real-time).
  - **Media Storage:** Lưu trữ tệp tin (MinIO/R2/Cloudinary).

---

## 2. Luồng Sự kiện (Event Flow Diagram)

Sơ đồ này mô tả cơ chế **Side-effects** khi một hành động chính diễn ra.

**Ví dụ: Quy trình phê duyệt bài nộp (Approve Submission)**

1.  **Moderator Action:** Gửi lệnh `APPROVE` qua API.
2.  **PostgreSQL Update:** Cập nhật trạng thái bài nộp và cộng điểm tích lũy cho người dùng.
3.  **Redis Update:** Cập nhật ngay lập tức điểm số của người dùng trong **Sorted Set (ZSET)** để làm mới Leaderboard.
4.  **MongoDB Update:** Ghi nhận sự kiện vào `activity_feed` để hiển thị trên bảng tin cộng đồng.
5.  **Notification Trigger:** Hệ thống tạo thông báo và đẩy qua **WebSocket** tới người dùng liên quan.

**Ví dụ: Quy trình gửi tin nhắn chat (Messaging)**

1. **User Action:** Gửi tin nhắn vào DM hoặc Challenge Channel qua API.
2. **MongoDB Write:** Lưu document `chat_messages`, cập nhật `chat_conversations.last_message`.
3. **Membership/Unread Update:** Cập nhật trạng thái đã đọc/chưa đọc cho các thành viên hội thoại.
4. **WebSocket Push:** Phát sự kiện real-time tới các subscriber của conversation.
5. **Fallback Notification:** Nếu recipient offline (đặc biệt với DM), tạo notification để user nhận lại qua API khi online.

---

## 3. Sơ đồ Tuần tự (Sequence Diagrams)

### 3.1. Luồng Upload Media & Nộp bài (Direct-to-Storage)

Đây là quy trình quan trọng nhất để tối ưu băng thông cho server VPS.

| Bước | Bên gửi | Bên nhận | Hành động                                          |
| :--- | :------ | :------- | :------------------------------------------------- |
| 1    | Client  | Backend  | Yêu cầu Signed URL (kèm file metadata).            |
| 2    | Backend | Storage  | Tạo URL có chữ ký thông qua `MediaStorageService`. |
| 3    | Backend | Client   | Trả về Signed URL và File Key duy nhất.            |
| 4    | Client  | Storage  | Upload trực tiếp tệp tin lên Cloud Storage.        |
| 5    | Client  | Backend  | Gửi yêu cầu nộp bài chính thức kèm `media_id`.     |
| 6    | Backend | Postgres | Xác thực và lưu trữ bản ghi hoàn chỉnh vào DB.     |

### 3.2. Luồng Xếp hạng Thời gian thực (Leaderboard)

- **Ghi:** Mọi thay đổi điểm số ở Postgres đều được đồng bộ sang Redis ZSET.
- **Đọc:** Khi người dùng xem bảng xếp hạng, Backend truy vấn trực tiếp từ Redis để lấy danh sách IDs, sau đó "Hydrate" (lấy thêm tên/avatar) từ Postgres nếu cần.

---

## 4. Chỉ dẫn triển khai cho Copilot

- **Data Consistency:** Luôn thực hiện cập nhật Postgres trước khi cập nhật Redis/Mongo.
- **Async Processing:** Cân nhắc sử dụng `@Async` cho việc ghi log MongoDB để không chặn luồng chính của người dùng.
- **Abstraction:** Tuyệt đối không gọi trực tiếp thư viện Cloudinary hay MinIO trong Service nghiệp vụ, phải thông qua `MediaStorageService` interface.
