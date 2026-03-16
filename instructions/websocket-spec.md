<!-- instructions/websocket-spec.md -->

# ChallengeHub: WebSocket & STOMP Specification

Tài liệu này định nghĩa chuẩn giao tiếp real-time qua WebSocket/STOMP cho ChallengeHub. Mục tiêu là đồng bộ với API REST, thống nhất payload và cơ chế xác thực.

---

## 1. Tổng quan

- **Endpoint WebSocket:** `/ws` (Nginx reverse proxy → backend)
- **Protocol:** STOMP over WebSocket (có thể fallback SockJS cho trình duyệt cũ).
- **Auth:** Sử dụng **JWT Access Token** trong header hoặc query param khi handshake.
- **Đối tượng chính:**
  - Thông báo (notifications)
  - Messaging realtime (DM + Challenge Channels)
  - Cập nhật typing/read-receipt cho chat
  - Cập nhật leaderboard (tùy chọn phase sau)
  - Sự kiện social (comment, reaction) — phase sau

Tại MVP, WebSocket phục vụ **notifications + messaging real-time**.

---

## 2. Xác thực WebSocket

### 2.1. Cách client gửi JWT khi kết nối

Frontend phải gửi Access Token (AT) hiện tại trong handshake:

- **Cách 1 (khuyến nghị):** Header `Authorization: Bearer <access_token>`
- **Cách 2:** Query param `?token=<access_token>` (fallback nếu không set header được).

Backend phải:

1. Parse JWT như HTTP request bình thường.
2. Validate chữ ký, hạn sử dụng, blacklist (Redis) giống filter HTTP.
3. Nếu hợp lệ → xác định `userId`, `role` và bind vào `Principal` của session.
4. Nếu không hợp lệ → từ chối kết nối.

### 2.2. Refresh Token

- **Refresh Token KHÔNG bao giờ đi qua WebSocket.**
- Nếu AT hết hạn trong khi đang kết nối:
  - Frontend tự refresh qua REST `/api/v1/auth/refresh`.
  - Sau khi có AT mới, client **disconnect + reconnect** WebSocket với token mới.

---

## 3. Định tuyến STOMP

### 3.1. Prefix chuẩn

- `app` prefix (client → server): `/app/**`
- `topic` prefix (broadcast): `/topic/**`
- `user` prefix (user-specific): `/user/**` (Spring sẽ map thành `/user/{session}/...`).

### 3.2. Kênh cho Notifications

#### Subscribe

Client subscribe notifications cá nhân qua:

- Destination: `/user/queue/notifications`

Spring mapping đề xuất:

- `userDestinationPrefix = "/user"`
- Broker prefix: `/topic`, `/queue`

#### Payload Notification

Payload gửi qua STOMP frame dạng JSON, khớp với schema REST `/api/v1/notifications`:

```json
{
  "id": "objectid",
  "type": "SUBMISSION_APPROVED",
  "title": "Bài nộp đã được duyệt",
  "message": "Bài nộp ngày 5 của Challenge X đạt 8/10 điểm",
  "metadata": {
    "submission_id": "uuid",
    "challenge_id": "uuid"
  },
  "read": false,
  "created_at": "2026-03-13T10:27:12Z"
}
```

- `read`: luôn `false` cho event push mới.
- Client sau đó có thể gọi REST `PATCH /api/v1/notifications/{id}/read` để đánh dấu đã đọc.

### 3.3. Kênh cho Messaging (DM + Challenge Channel)

#### Subscribe

Client subscribe các channel sau:

- Inbox updates (cá nhân): `/user/queue/chat/inbox`
- Message stream theo conversation: `/topic/chat/{conversationId}/messages`
- Typing indicator theo conversation: `/topic/chat/{conversationId}/typing`
- Read receipt theo conversation: `/topic/chat/{conversationId}/read-receipts`

> `conversationId` có thể là DM hoặc channel thuộc challenge. Backend phải kiểm tra membership trước khi cho phép subscribe.

#### Client Send (Typing)

- Client gửi event typing (transient) vào:
  - `/app/chat/{conversationId}/typing`

Payload ví dụ:

```json
{
  "is_typing": true
}
```

Server sẽ broadcast tới `/topic/chat/{conversationId}/typing` (không lưu DB).

#### Payload Message Event

```json
{
  "event": "CHAT_MESSAGE_CREATED",
  "conversation_id": "objectid",
  "payload": {
    "id": "objectid",
    "sender": {
      "id": "uuid",
      "username": "string",
      "avatar_url": "string|null"
    },
    "type": "TEXT",
    "content": "Xin chào mọi người",
    "attachments": [],
    "created_at": "2026-03-13T10:27:12Z"
  }
}
```

- `event` có thể là: `CHAT_MESSAGE_CREATED`, `CHAT_MESSAGE_UPDATED`, `CHAT_MESSAGE_DELETED`.

#### Payload Inbox Update

```json
{
  "event": "CHAT_INBOX_UPDATED",
  "conversation_id": "objectid",
  "unread_count": 4,
  "last_message": {
    "id": "objectid",
    "sender_id": "uuid",
    "content_preview": "Xin chào mọi người",
    "sent_at": "2026-03-13T10:27:12Z"
  }
}
```

### 3.4. Các event mở rộng (Phase sau)

Có thể bổ sung sau:

- `/topic/challenges/{challengeId}/leaderboard` — cập nhật leaderboard realtime.
- `/topic/submissions/{submissionId}/comments` — stream comment mới.

---

## 4. Hành vi phía Backend

### 4.1. Khi tạo notification (business event)

Khi backend tạo document mới trong collection `notifications` (MongoDB):

1. Lưu document như bình thường.
2. Nếu user nhận (`recipient_id`) đang online (có WebSocket session):
   - Gửi STOMP message tới `/user/{recipient}/queue/notifications`.
3. Không retry phức tạp: nếu offline → user sẽ nhận qua REST `/notifications` khi mở app.

### 4.2. Khi tạo chat message

Khi backend xử lý gửi message thành công qua REST:

1. Lưu document `chat_messages`.
2. Cập nhật `chat_conversations.last_message` và unread counters trong `chat_memberships`.
3. Broadcast message event tới `/topic/chat/{conversationId}/messages`.
4. Gửi inbox update tới từng user qua `/user/{recipient}/queue/chat/inbox`.
5. Nếu recipient offline (đặc biệt DM), có thể tạo `NEW_CHAT_MESSAGE` notification làm fallback.

Khi backend sửa/xóa message hợp lệ:

- Broadcast `CHAT_MESSAGE_UPDATED` hoặc `CHAT_MESSAGE_DELETED` tới `/topic/chat/{conversationId}/messages`.

### 4.3. Error Handling

Nếu việc gửi STOMP message thất bại:

- Không rollback transaction Postgres/Mongo.
- Log WARN; không ảnh hưởng đến logic chính.
- Client dựa trên polling REST để backup.

Nếu user subscribe conversation không thuộc membership:

- Backend từ chối subscribe hoặc disconnect session.
- Không được phát payload chat cho user không đủ quyền.

---

## 5. Frontend Integration (Tóm tắt)

- Sử dụng cùng Access Token từ **Zustand auth store**.
- Khi user login:
  - Lưu AT vào store.
  - Kết nối WebSocket với header `Authorization` từ AT.
  - Subscribe mặc định `/user/queue/notifications` + `/user/queue/chat/inbox`.
- Khi user mở màn hình chat conversation:
  - Subscribe `/topic/chat/{conversationId}/messages`, `/typing`, `/read-receipts`.
- Khi user đóng/move khỏi conversation:
  - Unsubscribe các topic của conversation cũ.
- Khi refresh token:
  - Cập nhật AT trong store.
  - Ngắt kết nối WebSocket cũ, kết nối lại với AT mới.

Không bao giờ lưu Refresh Token trong JS; chỉ dùng cookie HttpOnly cho refresh flow qua REST.
