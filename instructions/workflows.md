<!-- instructions/workflows.md -->

# ChallengeHub: Core Business Workflows

Tài liệu này đặc tả các quy trình nghiệp vụ (Workflows) của hệ thống ChallengeHub. Mỗi workflow mô tả actors, preconditions, flow chi tiết, và xử lý lỗi.

---

## 1. User Lifecycle & Authentication

### 1.1. Registration

- **Actors:** Guest (Khách).
- **Preconditions:** Email và Username chưa tồn tại.
- **Flow:**
  1. Frontend gửi `POST /api/v1/auth/register` kèm `username`, `email`, `password`.
  2. Backend validate (password policy: 8+ chars, 1 uppercase, 1 lowercase, 1 digit).
  3. Backend hash password (BCrypt, strength=10), lưu vào `users` (Postgres) với status=ACTIVE, role=USER.
  4. Trả về user profile (không bao gồm password).
  5. _(Optional)_ Gửi email welcome nếu `MAIL_ENABLED=true`.
- **Thất bại:** `VALIDATION_DUPLICATE_EMAIL`, `VALIDATION_DUPLICATE_USERNAME`, `VALIDATION_FAILED`.

### 1.2. Login

- **Actors:** Guest.
- **Flow:**
  1. Frontend gửi `POST /api/v1/auth/login` kèm `email`, `password`.
  2. Backend kiểm tra `locked_until` — nếu còn locked → `AUTH_ACCOUNT_LOCKED`.
  3. Backend kiểm tra `status` — nếu BANNED → `AUTH_ACCOUNT_BANNED`, SUSPENDED → `AUTH_ACCOUNT_SUSPENDED`.
  4. Backend verify password (BCrypt).
  5. Nếu sai: `login_failed_count += 1`. Nếu đạt 5 → set `locked_until = NOW() + 15 phút`.
  6. Nếu đúng: reset `login_failed_count = 0`, `locked_until = null`.
  7. Tạo Access Token (AT, 15 phút) + Refresh Token (RT, 7 ngày).
  8. Hash RT (SHA-256) → lưu vào Redis: `rt:<hash>` với `family_id` mới, SADD vào `rt_family_tokens:<family_id>` và `rt_user_families:<user_id>`. Set TTL 7 ngày.
  9. Set RT vào HttpOnly Secure Cookie `ch_refresh_token` với `Secure=true`, `HttpOnly=true`, `SameSite=Lax` (mặc định).
  10. Trả về AT + user info trong response body; FE lưu AT vào **Zustand auth store** (in-memory), không lưu vào localStorage/cookie.
- **Thất bại:** `AUTH_INVALID_CREDENTIALS`, `AUTH_ACCOUNT_LOCKED`, `AUTH_ACCOUNT_BANNED`, `AUTH_ACCOUNT_SUSPENDED`.

### 1.3. Token Refresh (Rotation)

- **Flow:**
  1. Frontend gửi `POST /api/v1/auth/refresh`. RT được đọc từ cookie.
  2. Backend hash RT (SHA-256) → tìm key `rt:<hash>` trong Redis.
  3. Nếu không tồn tại → `AUTH_TOKEN_INVALID`.
  4. Nếu `revoked = "1"` → **Replay detected!** Set `rt_family:<family_id>.blocked = "1"`, revoke tất cả RT trong family, blacklist AT. Trả về `AUTH_REFRESH_REPLAY`.
  5. Kiểm tra `rt_family:<family_id>.blocked` → nếu `"1"` → `AUTH_REFRESH_REPLAY`.
  6. Set `rt:<old_hash>.revoked = "1"` → tạo RT mới `rt:<new_hash>` cùng `family_id` → cấp AT mới.
  7. Set RT mới vào cookie (cùng config cookie như login), trả về AT mới; FE cập nhật lại Access Token trong Zustand.

### 1.4. Logout

- **Flow:**
  1. Frontend gửi `POST /api/v1/auth/logout`.
  2. Backend đọc RT từ cookie, hash → tìm `rt:<hash>` trong Redis → lấy `family_id`.
  3. Set `rt_family:<family_id>.blocked = "1"`.
  4. Lấy tất cả token hashes từ `rt_family_tokens:<family_id>` → xóa tất cả `rt:<hash>` keys.
  5. SREM `family_id` khỏi `rt_user_families:<user_id>`.
  6. Blacklist AT hiện tại (`jti`) vào Redis với TTL = thời gian AT còn lại.
  7. Xóa cookie `ch_refresh_token`.
  8. FE xoá Access Token khỏi Zustand và đóng WebSocket (nếu đang kết nối).

### 1.5. Profile & Avatar Management

- **Actors:** USER.
- **Flow:**
  1. User gửi `PUT /api/v1/users/me` kèm `display_name`, `bio`, `avatar_url`.
  2. Nếu thay Avatar: thực hiện luồng **Media Upload (mục 6)** trước, lấy `file_url` → gán vào `avatar_url`.
  3. Backend cập nhật `users` table (Postgres), set `updated_at = NOW()`.
  4. Trả về profile đã cập nhật kèm stats + badges.
- **Stats computation:** Query `user_challenges` (count), `submissions` (count, sum score), `user_badges`.

---

## 2. Challenge Lifecycle

### 2.1. Create Challenge (DRAFT)

- **Actors:** CREATOR.
- **Flow:**
  1. Creator gửi `POST /api/v1/challenges` kèm title, description, dates, settings.
  2. Backend tạo challenge với `status = DRAFT`, `creator_id = current_user.id`.
  3. Ghi audit log `CREATE_CHALLENGE` vào MongoDB.
- **Tiếp theo:** Creator thêm Tasks qua `POST /api/v1/challenges/{id}/tasks`.

### 2.2. Publish Challenge (DRAFT → PUBLISHED)

- **Actors:** CREATOR (owner).
- **Preconditions:** Có ³1 Task, có `start_date` và `end_date`, `start_date < end_date`.
- **Flow:**
  1. Creator gửi `PATCH /api/v1/challenges/{id}/status` với `{ "status": "PUBLISHED" }`.
  2. Backend validate preconditions. Nếu thiếu → `CHALLENGE_MISSING_TASKS` hoặc `CHALLENGE_MISSING_DATES`.
  3. Cập nhật status → PUBLISHED.
  4. Ghi audit log `CHANGE_CHALLENGE_STATUS`.

### 2.3. Revert to Draft (PUBLISHED → DRAFT)

- **Preconditions:** Chưa có user nào join.
- **Thất bại:** `CHALLENGE_HAS_PARTICIPANTS`.

### 2.4. Auto-Transition: PUBLISHED → ONGOING

- **Trigger:** `ChallengeStatusJob` (mỗi 1 phút).
- **Logic:** Tìm challenges có `status = PUBLISHED` AND `start_date <= NOW()` → set `status = ONGOING`.
- **Side effect:** Ghi audit log (actor = SYSTEM).

### 2.5. Auto-Transition: ONGOING → ENDED

- **Trigger:** `ChallengeStatusJob` (mỗi 1 phút).
- **Logic:** Tìm challenges có `status = ONGOING` AND `end_date <= NOW()` → set `status = ENDED`.
- **Side effect:** Cập nhật tất cả `user_challenges` với `status = ACTIVE` → set `status = DONE`.

### 2.6. Archive Challenge (ENDED → ARCHIVED)

- **Actors:** CREATOR (owner) / ADMIN.
- **Flow:** Chuyển `status = ARCHIVED`. Challenge vẫn hiển thị nhưng không thể join hay submit.

### 2.7. Browse & Join Challenge

- **Actors:** USER.
- **Preconditions:** Challenge ở trạng thái `PUBLISHED` hoặc `ONGOING` (nếu `allow_late_join = true`).
- **Flow:**
  1. User gửi `POST /api/v1/challenges/{id}/join`.
  2. Backend kiểm tra: challenge status, `max_participants`, unique constraint.
  3. Tạo `user_challenges` với `status = ACTIVE`, `total_score = 0`.
  4. Insert event `JOIN_CHALLENGE` vào `activity_feed` (MongoDB).
  5. Tạo notification cho Creator.
  6. Đồng bộ chat membership: thêm user vào tất cả `CHALLENGE_CHANNEL` của challenge (ít nhất `general`).
- **Thất bại:** `CHALLENGE_NOT_JOINABLE`, `CHALLENGE_ALREADY_JOINED`, `CHALLENGE_FULL`.

### 2.8. Quit Challenge (Rời khỏi Challenge)

- **Actors:** USER (đã join).
- **Preconditions:** `user_challenges.status = ACTIVE`.
- **Flow:**
  1. User gửi `POST /api/v1/challenges/{id}/quit`.
  2. Backend kiểm tra: user đã join và `status = ACTIVE`.
  3. Set `user_challenges.status = QUIT`.
  4. Insert event `QUIT_CHALLENGE` vào `activity_feed` (MongoDB).
  5. Tạo notification cho Creator.
  6. Đánh dấu `chat_memberships.left_at` cho toàn bộ challenge channels để user không gửi tin mới.
- **Lưu ý:** User không thể rejoin sau khi QUIT. Submissions trước đó vẫn giữ nguyên nhưng không tính điểm leaderboard.
- **Thất bại:** `CHALLENGE_NOT_JOINED`, `CHALLENGE_ALREADY_QUIT`.

---

## 3. Task & Submission System (Core Workflow)

### 3.1. Task Visibility

- **ALL_AT_ONCE:** Tất cả tasks visible khi challenge ONGOING.
- **DAILY_UNLOCK:** Task N visible khi `challenge.start_date + (N-1) days ≤ NOW()`.
- API trả về `is_unlocked: true|false` cho mỗi task.

### 3.2. Submit Task

- **Actors:** USER (đã join challenge, challenge ONGOING).
- **Flow:**
  1. _(Nếu cần media)_ User upload file qua luồng Media (mục 6) → nhận `media_id`.
  2. User gửi `POST /api/v1/submissions/tasks/{taskId}/submit` kèm `description`, `media_id`.
  3. Backend validate:
     - User đã join challenge chứa task này (query `user_challenges`) → nếu chưa: `SUBMISSION_NOT_PARTICIPANT`.
     - Challenge đang ONGOING → nếu không: `SUBMISSION_CHALLENGE_ENDED`.
     - Task đã unlocked → nếu chưa: `TASK_NOT_UNLOCKED`.
     - Chưa có submission cho task này → nếu có: `SUBMISSION_ALREADY_EXISTS`.
  4. Tạo submission với `status = PENDING`.
  5. Trả về submission object.

### 3.3. Resubmit (Nộp lại sau Rejected)

- **Preconditions:** Submission status = `REJECTED`, Challenge đang `ONGOING`.
- **Flow:**
  1. User gửi `PUT /api/v1/submissions/{id}` kèm `description`, `media_id` mới.
  2. Backend reset: `status = PENDING`, clear `score`, `reviewer_id`, `reviewed_at`, `reject_reason`.
  3. Cập nhật `description` và `media_id` nếu có thay đổi.
- **Thất bại:** `SUBMISSION_ALREADY_APPROVED`, `SUBMISSION_INVALID_RESUBMIT`, `SUBMISSION_CHALLENGE_ENDED`.

### 3.4. Moderate Submission

- **Actors:** MODERATOR / ADMIN.
- **Flow:**
  1. Mod xem danh sách pending: `GET /api/v1/submissions/pending`.
  2. Mod duyệt: `PATCH /api/v1/submissions/{id}/status`:
     - **APPROVED:** kèm `score` (0 ≤ score ≤ task.max_score).
     - **REJECTED:** kèm `reject_reason` (bắt buộc).
  3. Backend lưu `reviewer_id`, `reviewed_at`.
  4. Ghi audit log vào MongoDB.
  5. Tạo notification cho user (`SUBMISSION_APPROVED` hoặc `SUBMISSION_REJECTED`).
  6. _(Nếu MAIL_ENABLED)_ Gửi email thông báo.

### 3.5. Side Effects khi APPROVED (Transaction Chain)

Thực hiện **tuần tự trong 1 transaction** (Postgres) rồi **@Async** (Redis + MongoDB):

1. **[Postgres - Transactional]**
   - Cập nhật submission: `status = APPROVED`, `score`, `reviewer_id`, `reviewed_at`.
   - Cộng điểm: `user_challenges.total_score += score`.
   - Cập nhật streak: kiểm tra `streak_last_date` theo logic (xem requirements.md mục 5.2).
2. **[Redis - Async]**
   - `ZINCRBY leaderboard:<challenge_id> <score> <user_id>`.
3. **[MongoDB - Async]**
   - Insert `activity_feed` event type `COMPLETE_TASK`.
   - Insert `notification` cho user.
   - Kiểm tra badge conditions → insert `user_badges` (Postgres) + `activity_feed` event `EARN_BADGE`.

---

## 4. Social Engagement & Feed

### 4.1. Comment

- **Flow:**
  1. User gửi `POST /api/v1/interactions/submissions/{id}/comments` kèm `content`.
  2. Backend lưu vào `comments` collection (MongoDB) kèm `username` + `avatar_url` (denormalized để query nhanh).
  3. Tạo notification cho chủ submission (`NEW_COMMENT`).

### 4.2. Reaction (Toggle)

- **Flow:**
  1. User gửi `POST /api/v1/interactions/submissions/{id}/react` kèm `type`.
  2. Backend kiểm tra unique compound index `{ submission_id, user_id }`:
     - Chưa tồn tại → **Insert** reaction.
     - Đã tồn tại cùng type → **Delete** (toggle off).
     - Đã tồn tại khác type → **Update** type.
  3. Trả về trạng thái hiện tại + tổng reaction counts.
  4. Tạo notification cho chủ submission (`NEW_REACTION`) — chỉ khi insert mới.

### 4.3. Activity Feed

- **Flow:**
  1. User gửi `GET /api/v1/social/feed` (phân trang).
  2. Backend query `activity_feed` collection (MongoDB), sort by `created_at` desc.
  3. Hydrate user info (username, avatar) từ Postgres nếu cần.

---

## 5. Leaderboard & Ranking

### 5.1. Real-time Leaderboard

- **Ghi:** Mọi thay đổi điểm số ở Postgres đều đồng bộ sang Redis ZSET (xem 3.5).
- **Đọc:**
  1. User gửi `GET /api/v1/social/leaderboard/{challengeId}?top=50`.
  2. Backend query Redis ZSET: `ZREVRANGE leaderboard:<challenge_id> 0 49 WITHSCORES`.
  3. Hydrate user info (username, avatar) từ Postgres batch query.
  4. Nếu user đã đăng nhập → lấy `my_rank` qua `ZREVRANK`.

### 5.2. Leaderboard Rebuild (Recovery)

- Nếu Redis bị mất dữ liệu, rebuild từ Postgres:
  ```sql
  SELECT user_id, total_score FROM user_challenges
  WHERE challenge_id = ? AND status IN ('ACTIVE', 'DONE')
  ORDER BY total_score DESC
  ```
- Load vào Redis ZSET.

---

## 6. Media Storage Workflow

### 6.1. Upload Flow (Client-Direct)

1. **Bước 1:** User chọn file → Frontend gửi `POST /api/v1/media/upload-url` kèm `file_name`, `content_type`, `file_size`.
2. **Bước 2:** Backend validate (size, type) → gọi `MediaStorageService.generateUploadUrl()` → tạo record `media` (Postgres) với trạng thái pending.
3. **Bước 3:** Backend trả về `{ media_id, upload_url, file_key, expires_in }`.
4. **Bước 4:** Frontend upload tệp trực tiếp lên Storage Provider (PUT signed URL).
5. **Bước 5:** Frontend gửi `POST /api/v1/media/confirm/{mediaId}` để xác nhận upload xong.
6. **Bước 6:** Backend verify file tồn tại trên storage → cập nhật `file_url` → trả về media object.

### 6.2. Delete Media

- Backend gọi `MediaStorageService.delete(fileKey)` → xóa record `media` (Postgres).
- Ghi audit log `DELETE_MEDIA`.

---

## 7. Admin & Moderation Workflows

### 7.1. User Management

- **Ban User:** Admin gửi `PATCH /api/v1/admin/users/{id}/status` kèm `{ "status": "BANNED", "reason": "..." }`.
  - Lấy tất cả families từ Redis `rt_user_families:<user_id>`.
  - Block từng family (`rt_family:<family_id>.blocked = "1"`), xóa tất cả RT keys trong mỗi family.
  - Xóa `rt_user_families:<user_id>`.
  - Ghi audit log `BAN_USER`.
- **Suspend User:** Tương tự BAN nhưng status = SUSPENDED (có thể phục hồi).
- **Activate User:** Chuyển status về ACTIVE.

### 7.2. Role Management

- Admin gửi `PATCH /api/v1/admin/users/{id}/role` kèm `{ "role": "CREATOR" }`.
- Ghi audit log `CHANGE_USER_ROLE`.

### 7.3. Content Moderation

- Admin/Mod có thể xóa comments hoặc media vi phạm.
- Ghi audit log cho mọi action.

---

## 8. Notification System

### 8.1. Notification Triggers

| Sự kiện                            | Type                | Recipient           |
| :--------------------------------- | :------------------ | :------------------ |
| User join challenge                | NEW_PARTICIPANT     | Creator             |
| User quit challenge                | PARTICIPANT_QUIT    | Creator             |
| Submission approved                | SUBMISSION_APPROVED | Submitter           |
| Submission rejected                | SUBMISSION_REJECTED | Submitter           |
| New comment                        | NEW_COMMENT         | Submission owner    |
| New reaction                       | NEW_REACTION        | Submission owner    |
| New chat message (offline/mention) | NEW_CHAT_MESSAGE    | Conversation member |
| Rank thay đổi (top 10)             | RANK_CHANGE         | User bị vượt        |
| Badge earned                       | BADGE_EARNED        | User đạt badge      |
| Streak sắp đứt (email)             | STREAK_WARNING      | User có streak > 0  |

### 8.2. Delivery Channels

1. **In-App:** Lưu vào MongoDB `notifications` collection. User query qua API.
2. **WebSocket (Real-time):** Đẩy notification tới client đang online qua STOMP/SockJS.
3. **Email (Optional):** Gửi cho một số event quan trọng nếu `MAIL_ENABLED=true`.

### 8.3. WebSocket Endpoint

- **URL:** `wss://challengehub.app/ws` (Nginx proxy tới backend)
- **Protocol:** STOMP over SockJS.
- **Subscribe:** `/user/{userId}/notifications`
- **Auth:** JWT token gửi trong handshake header.

---

## 9. Scheduled Jobs Summary

| Job                     | Schedule            | Mô tả                                            | Tham chiếu        |
| :---------------------- | :------------------ | :----------------------------------------------- | :---------------- |
| `ChallengeStatusJob`    | Mỗi 1 phút          | Auto-transition PUBLISHED→ONGOING, ONGOING→ENDED | Workflow 2.4, 2.5 |
| `StreakResetJob`        | 00:15 UTC hàng ngày | Reset streak_count cho users đứt chuỗi           | Req 5.2           |
| `StreakWarningEmailJob` | 20:00 UTC hàng ngày | Gửi email nhắc submit cho users có streak > 0    | Infra 6.3         |

---

## 10. Messaging Workflows

### 10.1. Open/Create Direct Conversation

- **Actors:** USER.
- **Flow:**
  1. User mở màn hình chat riêng với user B: `POST /api/v1/chat/direct/{targetUserId}`.
  2. Backend kiểm tra `targetUserId != currentUserId`.
  3. Tìm conversation DM theo cặp participant hash (A-B).
  4. Nếu chưa có → tạo `chat_conversations(type=DIRECT)` + `chat_memberships` cho 2 user.
  5. Trả về conversation summary để FE subscribe realtime.
- **Thất bại:** `CHAT_DM_SELF_NOT_ALLOWED`, `NOT_FOUND`.

### 10.2. Create Challenge Channel

- **Actors:** CREATOR (owner) / MODERATOR / ADMIN.
- **Flow:**
  1. Actor gửi `POST /api/v1/chat/challenges/{challengeId}/channels` với `{ name, channel_key }`.
  2. Backend validate quyền + uniqueness `{ challenge_id, channel_key }`.
  3. Tạo conversation type `CHALLENGE_CHANNEL`.
  4. Đồng bộ membership cho tất cả user đang `ACTIVE/DONE` trong challenge.
- **Thất bại:** `CHALLENGE_NOT_FOUND`, `CHAT_CHANNEL_ALREADY_EXISTS`, `FORBIDDEN`.

### 10.3. Send Message (DM hoặc Challenge Channel)

- **Actors:** USER (thành viên conversation).
- **Flow:**
  1. User gửi `POST /api/v1/chat/conversations/{conversationId}/messages`.
  2. Backend validate membership + rate limit + payload (text/attachment).
  3. Lưu `chat_messages` document.
  4. Cập nhật `chat_conversations.last_message` + `updated_at`.
  5. Tăng `unread_count` cho các thành viên còn lại; sender giữ `unread_count = 0`.
  6. Push STOMP event `CHAT_MESSAGE_CREATED` tới subscribers conversation.
  7. Nếu recipient offline hoặc có mention quan trọng → tạo notification `NEW_CHAT_MESSAGE`.
- **Thất bại:** `CHAT_MEMBER_REQUIRED`, `CHAT_EMPTY_MESSAGE`, `CHAT_RATE_LIMITED`, `CHAT_CONVERSATION_NOT_FOUND`.

### 10.4. Read Conversation

- **Actors:** USER (thành viên conversation).
- **Flow:**
  1. FE gọi `PATCH /api/v1/chat/conversations/{conversationId}/read` với `last_message_id`.
  2. Backend cập nhật `last_read_message_id`, `last_read_at`, reset `unread_count = 0` trong `chat_memberships`.
  3. Backend push event `CHAT_READ_RECEIPT` để đồng bộ UI participant khác (nếu cần).

### 10.5. Edit/Delete Message

- **Actors:** USER (owner trong 15 phút) / MODERATOR / ADMIN.
- **Flow sửa message:**
  1. Owner gọi `PATCH /api/v1/chat/messages/{messageId}` với `content` mới.
  2. Backend kiểm tra cửa sổ 15 phút kể từ `created_at`.
  3. Cập nhật `content`, set `edited_at`.
  4. Push event `CHAT_MESSAGE_UPDATED` realtime.
- **Flow xóa message:**
  1. Actor gọi `DELETE /api/v1/chat/messages/{messageId}`.
  2. Backend kiểm tra quyền xóa.
  3. Soft-delete message (`deleted_at`), giữ `message_id` để không vỡ luồng phân trang.
  4. Push event `CHAT_MESSAGE_DELETED` realtime.
- **Thất bại:** `CHAT_MESSAGE_NOT_FOUND`, `CHAT_MESSAGE_EDIT_WINDOW_EXPIRED`, `CHAT_EMPTY_MESSAGE`, `FORBIDDEN`.
