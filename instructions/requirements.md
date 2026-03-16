<!-- instructions/requirements.md -->

# ChallengeHub: Detailed Business Requirements

Tài liệu này định nghĩa toàn bộ quy tắc nghiệp vụ (Business Rules) cho hệ thống ChallengeHub, phục vụ việc phát triển Backend (Spring Boot) và Frontend (React TS).

---

## 1. Tổng quan hệ thống

ChallengeHub là nền tảng thúc đẩy phát triển kỹ năng thông qua thử thách (Gamification).

- **Mô hình:** Web Responsive (Sẵn sàng API cho Mobile App).
- **Kiến trúc:** Modular Monolith (Spring Boot) + Hybrid DB.
- **Hosting:** VPS (Dockerized recommended).
- **Timezone chuẩn:** UTC cho tất cả timestamp. Frontend tự convert sang timezone local của user.

---

## 2. Quản lý Người dùng & Bảo mật

### 2.1. Phân quyền (RBAC)

Hệ thống sử dụng 4 roles chính thức. Đây là nguồn duy nhất về role naming — tất cả files khác phải tham chiếu bảng này.

| Role          | Quyền hạn                                                                  |
| :------------ | :------------------------------------------------------------------------- |
| **USER**      | Tham gia challenge, submit task, xem leaderboard, quản lý profile cá nhân. |
| **CREATOR**   | Bao gồm quyền USER + Tạo và quản lý Challenge của riêng mình.              |
| **MODERATOR** | Duyệt/Từ chối submission, ẩn nội dung vi phạm, quản lý báo cáo.            |
| **ADMIN**     | Toàn quyền hệ thống, quản lý user, cấu hình hệ thống, xem Audit Logs.      |

**Lưu ý:** Role là cột đơn trên bảng `users`. Tại MVP không hỗ trợ multi-role. CREATOR kế thừa quyền USER. ADMIN kế thừa mọi quyền.

### 2.2. Luồng Authentication

- Sử dụng **Spring Security + JWT**.
- **OAuth2:** Hỗ trợ login qua Google/GitHub (Sẽ mở rộng sau MVP).

#### JWT Token Durations & Storage Strategy

| Token Type    | Thời hạn    | Lưu trữ phía Client                           |
| :------------ | :---------- | :-------------------------------------------- |
| Access Token  | **15 phút** | \*\*Zustand (in-memory) trên FE, không cookie |
| Refresh Token | **7 ngày**  | HttpOnly Secure Cookie, không đọc được từ JS  |

- AT chứa claims: `sub` (user_id), `role`, `jti` (unique ID), `iat`, `exp`.
- RT không chứa claims — chỉ là opaque string, hash SHA-256 lưu trong **Redis** (xem db-schema.md mục 3.1).

**Quy tắc lưu trữ phía FE:**

- Access Token chỉ được lưu trong **Zustand store (in-memory)**, không ghi vào `localStorage`/`sessionStorage`, không set cookie cho AT.
- Refresh Token chỉ tồn tại trong cookie HttpOnly do backend set, không bao giờ truy cập từ JavaScript.
- WebSocket sử dụng Access Token từ Zustand để xác thực trong handshake (xem `websocket-spec.md`).

**Cấu hình cookie Refresh Token (`ch_refresh_token`):**

- `HttpOnly = true`.
- `Secure = true` (bắt buộc trên HTTPS production).
- `SameSite = Lax` là mặc định khuyến nghị; có thể cấu hình `Strict` cho môi trường yêu cầu bảo mật cao hơn nhưng cần kiểm tra lại các luồng redirect/OAuth2.
- `Path = /` hoặc giới hạn `/api/v1/auth` tùy chiến lược.

#### Refresh Token Rotation Family (Redis-based)

Hệ thống sử dụng cơ chế **Refresh Token Rotation** kết hợp **Token Family**, toàn bộ lưu trong **Redis** với TTL tự động dọn token hết hạn:

- **Login:** Tạo Access Token (AT) + Refresh Token (RT). RT hash được lưu vào Redis với `family_id` duy nhất cho mỗi phiên login.
- **Refresh:** Client gửi RT cũ → Backend revoke RT cũ, tạo RT mới cùng `family_id`, cấp AT mới.
- **Replay Detection:** Nếu RT đã revoke bị sử dụng lại → **Block toàn bộ family** (set `blocked = "1"` trong Redis), blacklist AT hiện tại. Đây là dấu hiệu token replay attack.
- **Logout:** Block family + revoke all RT trong family + blacklist AT (`jti`) vào Redis.
- **Ban/Suspend:** Lấy tất cả families của user từ Redis → block từng family → xóa tất cả RT keys.
- **AT Validation:** Mỗi request kiểm tra `jti` có nằm trong Redis blacklist không.
- **RT hợp lệ:** `rt:<hash>` tồn tại AND `revoked = "0"` AND `rt_family:<family_id>.blocked = "0"`.
- **TTL:** Tất cả Redis keys có TTL = 7 ngày → tự động dọn, không cần Scheduled Job cleanup.

### 2.3. Password Policy

| Quy tắc          | Giá trị                               |
| :--------------- | :------------------------------------ |
| Độ dài tối thiểu | 8 ký tự                               |
| Độ dài tối đa    | 128 ký tự                             |
| Yêu cầu          | Ít nhất 1 chữ hoa, 1 chữ thường, 1 số |
| Hash algorithm   | BCrypt (strength = 10)                |

### 2.4. Account Lockout

| Quy tắc                            | Giá trị         |
| :--------------------------------- | :-------------- |
| Số lần login sai tối đa            | 5 lần liên tiếp |
| Thời gian khóa tài khoản           | 15 phút         |
| Reset counter khi login thành công | Về 0            |

- Lưu `login_failed_count` và `locked_until` trong bảng `users`.
- Khi `locked_until > NOW()` → trả về error `AUTH_ACCOUNT_LOCKED`.

### 2.6. Authorization Matrix (RBAC chi tiết)

Bảng dưới đây chuẩn hóa quyền cho các hành động chính. Đây là nguồn tham chiếu cho controller/service khi kiểm tra quyền.

| Hành động                                  | Endpoint chính                                             | USER             | CREATOR (owner)        | MODERATOR        | ADMIN |
| :----------------------------------------- | :--------------------------------------------------------- | :--------------- | :--------------------- | :--------------- | :---- |
| Đăng ký, đăng nhập, refresh, logout        | `/auth/*`                                                  | ✔️               | ✔️                     | ✔️               | ✔️    |
| Xem profile bản thân                       | `GET /users/me`                                            | ✔️               | ✔️                     | ✔️               | ✔️    |
| Cập nhật profile bản thân                  | `PUT /users/me`                                            | ✔️               | ✔️                     | ✔️               | ✔️    |
| Xem profile public người khác              | `GET /users/{id}/stats`                                    | ✔️               | ✔️                     | ✔️               | ✔️    |
| Tạo challenge                              | `POST /challenges`                                         | ✖️               | ✔️                     | ✖️               | ✔️    |
| Sửa/Xóa challenge DRAFT (owner)            | `PUT/DELETE /challenges/{id}`                              | ✖️               | ✔️ (owner)             | ✖️               | ✔️    |
| Đổi trạng thái challenge (DRAFT↔PUBLISHED) | `PATCH /challenges/{id}/status`                            | ✖️               | ✔️ (owner)             | ✖️               | ✔️    |
| Archive challenge ENDED                    | `PATCH /challenges/{id}/status`                            | ✖️               | ✔️ (owner)             | ✖️               | ✔️    |
| Join/quit challenge                        | `POST /challenges/{id}/join`, `POST /challenges/{id}/quit` | ✔️               | ✔️                     | ✔️               | ✔️    |
| Xem tasks & submit bài                     | `/challenges/{id}/tasks`, `/submissions`                   | ✔️ (participant) | ✔️ (participant)       | ✔️ (participant) | ✔️    |
| Duyệt/Từ chối submission                   | `PATCH /submissions/{id}/status`                           | ✖️               | ✖️                     | ✔️               | ✔️    |
| Xem pending submissions                    | `GET /submissions/pending`                                 | ✖️               | ✖️                     | ✔️               | ✔️    |
| Bình luận, reaction trên submission        | `/interactions/submissions/*`                              | ✔️               | ✔️                     | ✔️               | ✔️    |
| Nhắn tin challenge channel                 | `/chat/challenges/*`, `/chat/conversations/*/messages`     | ✔️ (participant) | ✔️ (owner/participant) | ✔️               | ✔️    |
| Nhắn tin riêng (Direct Message)            | `/chat/direct/*`, `/chat/conversations/*/messages`         | ✔️               | ✔️                     | ✔️               | ✔️    |
| Sửa tin nhắn của mình (≤ 15 phút)          | `PATCH /chat/messages/{id}`                                | ✔️ (owner)       | ✔️ (owner)             | ✖️               | ✔️    |
| Tạo channel trong challenge                | `POST /chat/challenges/{id}/channels`                      | ✖️               | ✔️ (owner)             | ✔️               | ✔️    |
| Xóa tin nhắn vi phạm hoặc ngoài chính sách | `DELETE /chat/messages/{id}`                               | ✖️ (ngoài owner) | ✖️ (ngoài owner)       | ✔️               | ✔️    |
| Xóa bình luận người khác                   | `DELETE /interactions/comments/{id}`                       | ✖️               | ✖️                     | ✔️               | ✔️    |
| Quản lý users (role, status, list)         | `/admin/users/*`                                           | ✖️               | ✖️                     | ✖️               | ✔️    |
| Xem audit logs                             | `GET /admin/audit-logs`                                    | ✖️               | ✖️                     | ✖️               | ✔️    |

Ngoài bảng trên, mọi thao tác **PUT/PATCH/DELETE** trên resource thuộc sở hữu user (submission, profile, media, v.v.) phải kiểm tra thêm điều kiện **ownership** (`currentUser.id == resource.ownerId`) trừ khi role là ADMIN.

### 2.5. User Status

| Status      | Mô tả                                     | Hành vi                          |
| :---------- | :---------------------------------------- | :------------------------------- |
| `ACTIVE`    | Tài khoản bình thường                     | Truy cập đầy đủ                  |
| `BANNED`    | Bị cấm vĩnh viễn bởi Admin                | Từ chối mọi API, revoke tokens   |
| `SUSPENDED` | Tạm khóa (do vi phạm nhẹ hoặc tự yêu cầu) | Từ chối mọi API trừ GET /profile |

---

## 3. Module Thử thách (Challenge Module) - Core Logic

### 3.1. Vòng đời của Challenge (Lifecycle State Machine)

```text
  ┌──────────┐     publish()     ┌───────────┐
  │  DRAFT   │ ────────────────→ │ PUBLISHED │
  └──────────┘                   └───────────┘
       │                              │
       │ delete()                     │ start() [auto: start_date đến]
       ↓                              ↓
   [Xóa khỏi DB]               ┌──────────┐
                                │ ONGOING  │
                                └──────────┘
                                      │
                                      │ end() [auto: end_date đến]
                                      ↓
                                ┌──────────┐
                                │  ENDED   │
                                └──────────┘
                                      │
                                      │ archive()
                                      ↓
                                ┌──────────┐
                                │ ARCHIVED │
                                └──────────┘
```

#### Bảng Transition Rules

| Trạng thái hiện tại | Trạng thái đích | Ai được phép            | Trigger                                 |
| :------------------ | :-------------- | :---------------------- | :-------------------------------------- |
| DRAFT               | PUBLISHED       | CREATOR (owner)         | Manual API call                         |
| DRAFT               | [Deleted]       | CREATOR (owner)         | Manual API call                         |
| PUBLISHED           | ONGOING         | SYSTEM                  | Auto: `start_date` đến (Scheduled Job)  |
| PUBLISHED           | DRAFT           | CREATOR (owner)         | Manual (thu hồi nếu chưa có người join) |
| ONGOING             | ENDED           | SYSTEM                  | Auto: `end_date` đến (Scheduled Job)    |
| ENDED               | ARCHIVED        | CREATOR (owner) / ADMIN | Manual API call                         |

**Quy tắc:**

- DRAFT → PUBLISHED yêu cầu: có ít nhất 1 Task, có `start_date` và `end_date`, `start_date < end_date`.
- PUBLISHED → DRAFT chỉ được phép khi **chưa có user nào join**.
- Không được chuyển ngược từ ONGOING/ENDED/ARCHIVED → trạng thái trước.
- Scheduled Job chạy mỗi **1 phút** kiểm tra `start_date`/`end_date` để auto-transition.

### 3.2. Quy tắc tham gia (Join Logic)

- User chỉ có thể `JOIN` khi Challenge ở trạng thái `PUBLISHED` hoặc `ONGOING` (nếu `allow_late_join = true`).
- Khi Join, hệ thống khởi tạo bản ghi `UserChallenge` với status `ACTIVE`.
- Nếu `max_participants` đã đạt → trả về error `CHALLENGE_FULL`.
- Kiểm tra unique constraint `(user_id, challenge_id)` → nếu đã join → `CHALLENGE_ALREADY_JOINED`.

---

## 4. Hệ thống Task & Submission

### 4.1. Task System

- Challenge gồm nhiều Task (đánh số theo `day_number`).
- **Task Unlock Mode:**
  - `ALL_AT_ONCE`: Tất cả task mở ngay khi challenge ONGOING.
  - `DAILY_UNLOCK`: Task mở theo ngày. Task N mở khi `start_date + (N-1) days ≤ NOW()`.
- User không được submit task chưa unlock → error `TASK_NOT_UNLOCKED`.

### 4.2. Submission Rules (Chi tiết)

#### Tạo mới Submission

- User chỉ có thể submit nếu đã JOIN challenge chứa task đó (status = ACTIVE).
- Mỗi user chỉ có **1 submission duy nhất** cho mỗi task (unique constraint `task_id + user_id`).
- `media_id` là **tùy chọn** — submission có thể chỉ có description (text).
- Khi tạo → status = `PENDING`.

#### Resubmit Logic (Nộp lại sau khi bị Rejected)

- Khi submission bị `REJECTED`, user **cập nhật bài nộp hiện tại** (PUT) thay vì tạo mới.
- Resubmit chỉ được phép khi:
  - Status hiện tại = `REJECTED`.
  - Challenge vẫn đang ONGOING.
- Khi resubmit → status reset về `PENDING`, xóa `score`, `reviewer_id`, `reviewed_at`, `reject_reason`.

#### Không thể sửa sau khi APPROVED

- Submission đã APPROVED là **immutable** — không được sửa hoặc xóa.

#### Review Submission (Moderator)

- Chỉ MODERATOR hoặc ADMIN mới được duyệt.
- Duyệt = set status `APPROVED` + gán `score` (0 ≤ score ≤ task.max_score).
- Từ chối = set status `REJECTED` + gán `reject_reason` (bắt buộc).
- Lưu `reviewer_id` và `reviewed_at`.
- Ghi audit log vào MongoDB.

### 4.3. Side Effects khi APPROVED

Khi submission được APPROVED, hệ thống thực hiện:

**Bước 1 — Postgres (Transactional, đồng bộ):**

1. Cập nhật submission: `status = APPROVED`, `score`, `reviewer_id`, `reviewed_at`.
2. Cộng `score` vào `user_challenges.total_score`.
3. Kiểm tra và cập nhật `users.streak_count` (xem mục 5.2).

**Bước 2 — Async (@Async, không chặn luồng chính):**

4. **Redis:** `ZINCRBY leaderboard:<challenge_id> <score> <user_id>`.
5. **MongoDB:** Insert event vào `activity_feed`.
6. **MongoDB:** Tạo notification cho user.
7. **MongoDB/Postgres:** Kiểm tra và cấp badge nếu đủ điều kiện.

---

## 5. Hệ thống Tính điểm & Leaderboard

### 5.1. Scoring

- Điểm tổng per challenge = `user_challenges.total_score` = Σ(submission.score) cho mỗi task approved.
- Điểm Task: do Moderator chấm thủ công (0 → task.max_score).
- Auto-scoring (chấm code tự động) — **Phase 2**, không implement tại MVP.

### 5.2. Streak System

#### Định nghĩa

- **Streak** = số ngày liên tục mà user có ít nhất 1 submission được `APPROVED`.
- Tính theo **UTC date** (không phụ thuộc timezone của user).

#### Cơ chế cập nhật (Khi submission được APPROVED)

```text
today = current UTC date
last  = users.streak_last_date

IF last == today:
    → Không thay đổi (đã submit hôm nay rồi)
ELSE IF last == today - 1:
    → streak_count += 1  (tiếp tục chuỗi)
    → streak_last_date = today
ELSE:
    → streak_count = 1   (bắt đầu chuỗi mới)
    → streak_last_date = today
```

**Yêu cầu về tính đồng thời (Concurrency):**

- Toàn bộ cập nhật streak phải nằm trong cùng **transaction** với việc APPROVE submission (`@Transactional` trên service chấm bài).
- Update phải đảm bảo **atomic** để tránh double-increment khi có nhiều APPROVE trong cùng một ngày cho cùng một user (ví dụ hai submission approved gần như đồng thời):
  - Có thể dùng `SELECT ... FOR UPDATE` trên bản ghi user trước khi tính toán streak.
  - Hoặc dùng câu lệnh `UPDATE users SET streak_count = ..., streak_last_date = ... WHERE id = ? AND streak_last_date = :expectedLastDate` rồi kiểm tra số hàng bị ảnh hưởng.
- Mục tiêu: với bất kỳ số lượng submission APPROVED trong cùng một ngày, streak chỉ tăng **tối đa 1 lần/ngày** cho mỗi user.

#### Streak Reset Cron Job

- **Schedule:** Chạy hàng ngày lúc `00:15 UTC`.
- **Logic:** Tìm tất cả users có `streak_last_date < today - 1` AND `streak_count > 0` → set `streak_count = 0`.
- **Lý do chạy 00:15 thay vì 00:00:** Tránh race condition với các submission approved vào đúng nửa đêm.

#### Streak Milestones & Other Badges

| Badge Code   | Điều kiện                                | Badge Name         |
| :----------- | :--------------------------------------- | :----------------- |
| FIRST_SUBMIT | Lần đầu tiên có submission được APPROVED | Lần đầu xuất sắc   |
| STREAK_3     | streak_count đạt 3                       | Bắt đầu khởi động  |
| STREAK_7     | streak_count đạt 7                       | Chiến binh tuần lễ |
| STREAK_14    | streak_count đạt 14                      | Kiên trì 2 tuần    |
| STREAK_30    | streak_count đạt 30                      | Huyền thoại tháng  |

- Badge được kiểm tra tự động trong side-effect khi submission APPROVED.
- Mỗi badge chỉ cấp 1 lần (unique constraint `user_id + badge_id`).
- Khi cấp badge: insert `user_badges` (Postgres) + `activity_feed` event `EARN_BADGE` + `STREAK_MILESTONE` (MongoDB).

---

## 6. Ghi log & Giám sát (Audit Logs)

Mọi thay đổi nhạy cảm phải được ghi vào **MongoDB** collection `audit_logs`:

- **Actor:** Ai thực hiện (User ID, Role).
- **Action:** Hành động (enum — xem danh sách bên dưới).
- **Resource:** Type + ID của đối tượng bị tác động.
- **Timestamp:** Thời gian thực thi (UTC).
- **Payload:** Dữ liệu cũ và mới (Dạng JSON).
- **IP Address** + **User Agent**.

#### Danh sách Audit Actions:

| Action                  | Resource Type | Mô tả                       |
| :---------------------- | :------------ | :-------------------------- |
| CREATE_CHALLENGE        | CHALLENGE     | Tạo challenge mới           |
| UPDATE_CHALLENGE        | CHALLENGE     | Cập nhật thông tin          |
| CHANGE_CHALLENGE_STATUS | CHALLENGE     | Chuyển trạng thái lifecycle |
| APPROVE_SUBMISSION      | SUBMISSION    | Duyệt bài nộp               |
| REJECT_SUBMISSION       | SUBMISSION    | Từ chối bài nộp             |
| BAN_USER                | USER          | Cấm user                    |
| SUSPEND_USER            | USER          | Tạm khóa user               |
| CHANGE_USER_ROLE        | USER          | Thay đổi role               |
| DELETE_COMMENT          | COMMENT       | Xóa bình luận               |
| DELETE_MEDIA            | MEDIA         | Xóa media                   |
| DELETE_CHAT_MESSAGE     | CHAT_MESSAGE  | Xóa tin nhắn vi phạm        |

---

## 7. Quy tắc bảo mật dữ liệu (Security Rules)

- **ID Obfuscation:** Sử dụng UUID cho các ID công khai trên URL để tránh bị quét dữ liệu (IDOR).
- **Data Validation:** Validate chặt chẽ phía Backend bằng Hibernate Validator (`@NotNull`, `@Size`, `@Pattern`, `@Email`).
- **CORS:** Chỉ cho phép origin từ domain frontend (`challengehub.app` và `localhost:5173` cho dev).
- **HTTPS:** Bắt buộc trên production. Nginx handle SSL termination.
- **Input Sanitization:** Chống XSS cho các field text/description (sanitize HTML tags).

### 7.1. Rate Limiting

| Scope                 | Limit        | Window |
| :-------------------- | :----------- | :----- |
| API Public (chung)    | 100 requests | 1 phút |
| Login endpoint        | 10 requests  | 1 phút |
| Register endpoint     | 5 requests   | 1 phút |
| Upload URL            | 10 requests  | 1 phút |
| Messaging send        | 30 requests  | 1 phút |
| Authenticated (chung) | 300 requests | 1 phút |

---

## 8. Error Code Catalog

Tất cả API trả về response theo chuẩn `ApiResponse`. Khi có lỗi, field `errorCode` phải sử dụng một trong các mã dưới đây.

### 8.0. Backend Enum Contract

Trong backend, các mã lỗi được chuẩn hóa bằng enum `ErrorCode` để tái sử dụng xuyên suốt service + exception handler.

- Vị trí: `challenge-hub-backend/src/main/java/com/challengehub/exception/ErrorCode.java`
- Cấu trúc mỗi enum value:
  - `code` (số nguyên)
  - `message` (mặc định)
  - `statusCode` (HTTP status)

`ApiException` nhận `ErrorCode` và có thể override message khi cần ngữ cảnh cụ thể.

Ví dụ:

```java
throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
throw new ApiException(ErrorCode.SUBMISSION_SCORE_EXCEEDED, "Score vuot qua max_score cua task");
```

### 8.1. Authentication Errors (AUTH_xxx)

| Error Code               | HTTP Status | Mô tả                                    |
| :----------------------- | :---------- | :--------------------------------------- |
| AUTH_INVALID_CREDENTIALS | 401         | Email/password sai                       |
| AUTH_ACCOUNT_LOCKED      | 423         | Tài khoản bị khóa do login sai nhiều lần |
| AUTH_ACCOUNT_BANNED      | 403         | Tài khoản bị cấm                         |
| AUTH_ACCOUNT_SUSPENDED   | 403         | Tài khoản bị tạm khóa                    |
| AUTH_TOKEN_EXPIRED       | 401         | Access Token hết hạn                     |
| AUTH_TOKEN_INVALID       | 401         | Token không hợp lệ hoặc bị blacklist     |
| AUTH_TOKEN_REVOKED       | 401         | Refresh Token đã bị thu hồi              |
| AUTH_REFRESH_REPLAY      | 401         | Phát hiện token replay — family bị block |

### 8.2. Validation Errors (VALIDATION_xxx)

| Error Code                    | HTTP Status | Mô tả                                   |
| :---------------------------- | :---------- | :-------------------------------------- |
| VALIDATION_FAILED             | 400         | Dữ liệu không hợp lệ (kèm field errors) |
| VALIDATION_DUPLICATE_EMAIL    | 409         | Email đã tồn tại                        |
| VALIDATION_DUPLICATE_USERNAME | 409         | Username đã tồn tại                     |

### 8.3. Challenge Errors (CHALLENGE_xxx)

| Error Code                   | HTTP Status | Mô tả                                      |
| :--------------------------- | :---------- | :----------------------------------------- |
| CHALLENGE_NOT_FOUND          | 404         | Không tìm thấy challenge                   |
| CHALLENGE_ALREADY_JOINED     | 409         | User đã tham gia challenge này             |
| CHALLENGE_FULL               | 409         | Đã đạt số người tham gia tối đa            |
| CHALLENGE_NOT_JOINABLE       | 403         | Challenge không ở trạng thái cho phép join |
| CHALLENGE_NOT_JOINED         | 400         | User chưa tham gia challenge này           |
| CHALLENGE_ALREADY_QUIT       | 409         | User đã rời khỏi challenge                 |
| CHALLENGE_INVALID_TRANSITION | 400         | Chuyển trạng thái không hợp lệ             |
| CHALLENGE_HAS_PARTICIPANTS   | 400         | Không thể revert vì đã có người join       |
| CHALLENGE_MISSING_TASKS      | 400         | Cần ít nhất 1 task để publish              |
| CHALLENGE_MISSING_DATES      | 400         | Thiếu start_date hoặc end_date             |

### 8.4. Submission Errors (SUBMISSION_xxx)

| Error Code                  | HTTP Status | Mô tả                                    |
| :-------------------------- | :---------- | :--------------------------------------- |
| SUBMISSION_NOT_FOUND        | 404         | Không tìm thấy bài nộp                   |
| SUBMISSION_NOT_PARTICIPANT  | 403         | User chưa join challenge chứa task này   |
| SUBMISSION_ALREADY_EXISTS   | 409         | Đã submit cho task này (dùng PUT để sửa) |
| SUBMISSION_ALREADY_APPROVED | 403         | Không thể sửa bài đã được duyệt          |
| SUBMISSION_CHALLENGE_ENDED  | 403         | Challenge đã kết thúc                    |
| SUBMISSION_INVALID_RESUBMIT | 403         | Chỉ được resubmit khi status = REJECTED  |
| SUBMISSION_SCORE_EXCEEDED   | 400         | Điểm vượt quá max_score của task         |

### 8.5. Task Errors (TASK_xxx)

| Error Code        | HTTP Status | Mô tả                       |
| :---------------- | :---------- | :-------------------------- |
| TASK_NOT_FOUND    | 404         | Không tìm thấy task         |
| TASK_NOT_UNLOCKED | 403         | Task chưa mở (Daily Unlock) |

### 8.6. General Errors

| Error Code          | HTTP Status | Mô tả                             |
| :------------------ | :---------- | :-------------------------------- |
| FORBIDDEN           | 403         | Không có quyền                    |
| NOT_FOUND           | 404         | Resource không tồn tại            |
| RATE_LIMITED        | 429         | Vượt quá giới hạn request         |
| INTERNAL_ERROR      | 500         | Lỗi hệ thống không xác định       |
| MEDIA_UPLOAD_FAILED | 500         | Không thể tạo signed URL          |
| MEDIA_TOO_LARGE     | 400         | File vượt quá kích thước cho phép |
| MEDIA_INVALID_TYPE  | 400         | Định dạng file không được phép    |

### 8.7. Messaging Errors (CHAT_xxx)

| Error Code                       | HTTP Status | Mô tả                                                    |
| :------------------------------- | :---------- | :------------------------------------------------------- |
| CHAT_CONVERSATION_NOT_FOUND      | 404         | Không tìm thấy conversation                              |
| CHAT_CHANNEL_NOT_FOUND           | 404         | Không tìm thấy channel trong challenge                   |
| CHAT_CHANNEL_ALREADY_EXISTS      | 409         | Channel key/name đã tồn tại trong challenge              |
| CHAT_MEMBER_REQUIRED             | 403         | User không phải thành viên conversation/channel          |
| CHAT_DM_SELF_NOT_ALLOWED         | 400         | Không thể tạo cuộc trò chuyện riêng với chính mình       |
| CHAT_EMPTY_MESSAGE               | 400         | Message phải có content hoặc attachment hợp lệ           |
| CHAT_MESSAGE_NOT_FOUND           | 404         | Không tìm thấy tin nhắn                                  |
| CHAT_MESSAGE_EDIT_WINDOW_EXPIRED | 403         | Đã quá thời gian cho phép sửa/xóa tin nhắn của chính chủ |
| CHAT_RATE_LIMITED                | 429         | Vượt quá giới hạn gửi tin nhắn                           |

---

## 9. Messaging & Realtime Chat

### 9.1. Mô hình hội thoại

- Hệ thống hỗ trợ 2 loại hội thoại:
  - `DIRECT`: Nhắn tin riêng giữa 2 user.
  - `CHALLENGE_CHANNEL`: Nhắn tin nhóm theo challenge (kiểu channel như Discord).
- Mỗi challenge có tối thiểu 1 channel mặc định: `general`.
- Có thể mở rộng thêm channel khác như `qna`, `showcase`, `announcements`.

### 9.2. Quy tắc Challenge Group Chat

- Khi challenge chuyển sang `PUBLISHED`, hệ thống phải đảm bảo channel mặc định `general` đã tồn tại.
- Khi user `JOIN challenge`, user được thêm vào membership của tất cả channel trong challenge.
- Khi user `QUIT challenge`, user mất quyền gửi tin mới trong challenge chat.
- User có `status = BANNED/SUSPENDED` không được gửi tin nhắn.
- Quyền tạo channel:
  - `CREATOR` (owner challenge), `MODERATOR`, `ADMIN`.
  - User thường không được tạo channel.

### 9.3. Quy tắc Direct Message (DM)

- Conversation DM được tạo theo cơ chế lazy: tạo khi user mở DM lần đầu hoặc gửi tin đầu tiên.
- Cặp DM phải unique theo cặp user (A-B và B-A là cùng 1 conversation).
- Không cho phép tự nhắn cho chính mình (`CHAT_DM_SELF_NOT_ALLOWED`).

### 9.4. Quy tắc Message

- Tin nhắn hỗ trợ:
  - Text (`1..2000` ký tự)
  - Attachment (tham chiếu `media_id` đã confirm)
- Message hợp lệ khi có ít nhất text hoặc 1 attachment hợp lệ (`CHAT_EMPTY_MESSAGE` nếu không).
- Áp dụng rate limit gửi tin theo user: `30 requests/phút`.
- Xóa/sửa tin nhắn:
  - Chủ tin nhắn có thể sửa/xóa trong cửa sổ 15 phút.
  - `MODERATOR/ADMIN` có thể xóa tin nhắn vi phạm bất kỳ lúc nào.

### 9.5. Read/Unread & Offline Delivery

- Hệ thống lưu `last_read_message_id` hoặc `last_read_at` theo từng user/conversation.
- Inbox trả về `unread_count` theo conversation và tổng `unread_count` toàn hệ thống chat.
- Nếu recipient offline, backend vẫn lưu message; user nhận lại qua API khi online.
- Với DM quan trọng (hoặc mention trong group), hệ thống có thể tạo thêm notification `NEW_CHAT_MESSAGE` để nhắc người dùng.

### 9.6. Realtime (WebSocket)

- Mọi message mới phải được push real-time tới thành viên conversation qua STOMP.
- Typing indicator là transient event, không lưu DB.
- Nếu push thất bại, không rollback message đã lưu.

---

## 10. Chỉ dẫn cho GitHub Copilot

- Luôn ưu tiên viết code xử lý logic nghiệp vụ trong tầng `@Service`.
- Tầng `@Controller` chỉ làm nhiệm vụ điều phối và validate request.
- Khi tạo Entity Postgres, hãy luôn cân nhắc việc tạo kèm một Document Class tương ứng cho MongoDB nếu dữ liệu đó cần truy xuất dạng Log/Feed.
- Sử dụng Error Codes từ mục 8 — **không tự đặt error code mới** mà không cập nhật catalog.
- Mọi scheduled job phải đăng ký trong `config/SchedulerConfig` và log kết quả.
