<!-- instructions/frontend-state.md -->

# ChallengeHub: Frontend State & Auth Strategy

Tài liệu này định nghĩa cấu trúc state phía frontend (React + TS), sử dụng **Zustand** cho global state và **TanStack Query** cho server state. Đồng thời, chuẩn hóa cách lưu Access Token và Refresh Token.

---

## 1. Nguyên tắc chung

- **Access Token (AT):** lưu trong **Zustand (in-memory)**, không lưu cookie, không lưu localStorage (tránh XSS).
- **Refresh Token (RT):** lưu trong **HttpOnly Secure Cookie** do backend set, **không truy cập được từ JS**.
- **TanStack Query:** quản lý toàn bộ server state (list, detail, pagination, v.v.).
- **Zustand:** quản lý auth state, UI state nhẹ, notification badge.

---

## 2. Auth Store (Zustand)

### 2.1. Shape đề xuất

```ts
export interface AuthUser {
  id: string;
  username: string;
  email: string;
  role: "USER" | "CREATOR" | "MODERATOR" | "ADMIN";
  avatarUrl: string | null;
  displayName: string | null;
}

export interface AuthState {
  accessToken: string | null;
  expiresAt: number | null; // epoch millis của AT
  user: AuthUser | null;
  isAuthenticated: boolean;

  setAuth: (payload: {
    accessToken: string;
    expiresIn: number; // giây, từ backend
    user: AuthUser;
  }) => void;

  clearAuth: () => void;
}
```

### 2.2. Hành vi

- `setAuth`:
  - Lưu `accessToken` vào state.
  - Tính `expiresAt = now + expiresIn * 1000`.
  - Lưu `user`, set `isAuthenticated = true`.
- `clearAuth`:
  - Xóa `accessToken`, `expiresAt`, `user`.
  - Set `isAuthenticated = false`.

**Không** lưu Refresh Token trong state hoặc bất cứ storage nào phía FE. RT chỉ tồn tại trong cookie HttpOnly.

---

## 3. Notification Store (Zustand)

```ts
export interface NotificationState {
  unreadCount: number;
  setUnreadCount: (count: number) => void;
  incrementUnread: () => void;
  decrementUnread: () => void;
}
```

- `unreadCount` đồng bộ với API `/api/v1/notifications/unread-count`.
- Khi nhận notification mới qua WebSocket:
  - Tăng `unreadCount`.
- Khi mark read / read-all:
  - Gọi API, sau đó set lại `unreadCount` theo response.

---

## 4. Messaging UI Store (Zustand)

```ts
export interface MessagingUiState {
  activeConversationId: string | null;
  typingUserIdsByConversation: Record<string, string[]>;
  setActiveConversation: (conversationId: string | null) => void;
  setTypingUsers: (conversationId: string, userIds: string[]) => void;
  clearTypingUsers: (conversationId: string) => void;
}
```

- Chỉ dùng store này cho **UI state tạm thời** (conversation đang mở, typing indicators).
- Không lưu message list vào Zustand; message list luôn lấy qua TanStack Query để đồng bộ cache + pagination.

---

## 5. TanStack Query Keys & Invalidation Rules

### 5.1. Query Keys

- `['auth', 'me']` → GET `/users/me`.
- `['challenges', 'list', params]` → GET `/challenges` với pagination/filter.
- `['challenges', 'detail', challengeId]` → GET `/challenges/{id}`.
- `['tasks', 'byChallenge', challengeId]` → GET `/challenges/{id}/tasks`.
- `['submissions', 'me', params]` → GET `/submissions/me`.
- `['leaderboard', challengeId]` → GET `/social/leaderboard/{challengeId}`.
- `['notifications', 'list', params]` → GET `/notifications`.
- `['notifications', 'unreadCount']` → GET `/notifications/unread-count`.
- `['chat', 'conversations', params]` → GET `/chat/conversations`.
- `['chat', 'messages', conversationId, params]` → GET `/chat/conversations/{conversationId}/messages`.
- `['chat', 'channels', challengeId]` → GET `/chat/challenges/{challengeId}/channels`.
- `['chat', 'unreadCount']` → GET `/chat/unread-count`.

### 5.2. Invalidation Rules (ví dụ chính)

- Sau `login` / `logout` / `refresh`:
  - `invalidateQueries(['auth', 'me'])`.
  - `invalidateQueries(['notifications', 'unreadCount'])`.
  - `invalidateQueries(['chat', 'unreadCount'])`.
- Sau khi **join/quit challenge**:
  - `invalidateQueries(['challenges', 'detail', challengeId])`.
  - `invalidateQueries(['leaderboard', challengeId])`.
  - `invalidateQueries(['chat', 'channels', challengeId])`.
  - `invalidateQueries(['chat', 'conversations'])`.
- Sau khi **submit/resubmit task**:
  - `invalidateQueries(['tasks', 'byChallenge', challengeId])`.
  - `invalidateQueries(['submissions', 'me'])`.
  - `invalidateQueries(['leaderboard', challengeId])`.
- Sau khi **moderate submission (approve/reject)**:
  - `invalidateQueries(['submissions', 'pending'])` (nếu có).
  - `invalidateQueries(['leaderboard', challengeId])`.
- Sau khi **gửi message**:
  - `invalidateQueries(['chat', 'messages', conversationId])`.
  - `invalidateQueries(['chat', 'conversations'])`.
  - `invalidateQueries(['chat', 'unreadCount'])` (nếu backend trả unread mới qua API).
- Sau khi **mark read conversation**:
  - `invalidateQueries(['chat', 'conversations'])`.
  - `invalidateQueries(['chat', 'unreadCount'])`.
  - `invalidateQueries(['notifications', 'unreadCount'])` (nếu dùng fallback notification cho DM offline).

---

## 6. Auth Flow trên FE

### 6.1. Login

1. User gọi `POST /api/v1/auth/login`.
2. Backend trả về `{ access_token, expires_in, user }` và set RT cookie.
3. FE gọi `authStore.setAuth({ accessToken, expiresIn, user })`.
4. Cấu hình Axios instance dùng `accessToken` từ store cho header `Authorization`.

### 6.2. Refresh Token

1. Khi request REST trả về `401` do AT hết hạn:
   - Gọi `/api/v1/auth/refresh` (với cookie RT tự gửi kèm).
2. Nếu refresh thành công:
   - Backend trả AT mới (`access_token`, `expires_in`) và set lại RT cookie.
   - FE cập nhật `authStore.setAuth` với AT mới, user giữ nguyên.
   - Retry request thất bại ban đầu.
3. Nếu refresh thất bại:
   - Gọi `authStore.clearAuth()` và redirect về màn hình login.

### 6.3. Logout

1. Gọi `POST /api/v1/auth/logout`.
2. Backend block RT family, blacklist AT, xóa cookie RT.
3. FE gọi `authStore.clearAuth()` và đóng WebSocket.

### 6.4. WebSocket cho Messaging

1. Sau login thành công, FE kết nối WebSocket bằng Access Token hiện tại.
2. FE luôn subscribe:

- `/user/queue/notifications`
- `/user/queue/chat/inbox`

3. Khi user mở 1 conversation, FE subscribe thêm:

- `/topic/chat/{conversationId}/messages`
- `/topic/chat/{conversationId}/typing`

4. Khi user chuyển conversation hoặc logout, FE unsubscribe các topic conversation cũ.
5. Khi Access Token được refresh, FE disconnect/reconnect WebSocket bằng token mới.

---

## 7. Cookie Config cho Refresh Token

Refresh Token được backend set vào cookie `ch_refresh_token` với cấu hình:

- `HttpOnly = true`
- `Secure = true` (bắt buộc trên HTTPS production)
- `SameSite = Lax` (default khuyến nghị)
- `Path = /` hoặc tối thiểu `/api/v1/auth`

**SameSite:**

- `Lax` phù hợp SPA thông thường, vẫn cho phép điều hướng top-level từ cùng domain.
- Có thể cấu hình `Strict` cho môi trường nhạy cảm hơn, nhưng cần kiểm tra kỹ các luồng OAuth2/redirect.

---

## 8. Tránh Anti-patterns

- Không lưu Access Token hoặc Refresh Token trong `localStorage`/`sessionStorage`.
- Không gắn Refresh Token vào header hoặc body request; luôn dùng cookie HttpOnly.
- Không đọc/ghi cookie RT từ JS.
- Không dùng Zustand để giữ lịch sử chat dài hạn; dùng TanStack Query (infinite/cursor) để tránh stale data và memory leak.
