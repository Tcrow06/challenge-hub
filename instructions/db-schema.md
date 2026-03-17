<!-- instructions/db-schema.md -->

# ChallengeHub: Database Schema Design

Tài liệu này quy định cấu trúc dữ liệu cho dự án, kết hợp giữa RDBMS (Postgres) và Document-based (MongoDB).

---

## 1. PostgreSQL (Core Business Logic)

Sử dụng cho các thực thể cần tính toàn vẹn dữ liệu cao và quan hệ chặt chẽ.

### 1.1. Bảng `users`

| Column               | Type         | Constraints      | Description                           |
| :------------------- | :----------- | :--------------- | :------------------------------------ |
| `id`                 | UUID         | PRIMARY KEY      | Sử dụng UUID để bảo mật URL           |
| `username`           | VARCHAR(50)  | UNIQUE, NOT NULL | Tên đăng nhập                         |
| `email`              | VARCHAR(100) | UNIQUE, NOT NULL | Email liên hệ                         |
| `password`           | VARCHAR(255) | NOT NULL         | BCrypt hashed password                |
| `role`               | VARCHAR(20)  | NOT NULL         | USER, CREATOR, MODERATOR, ADMIN       |
| `status`             | VARCHAR(20)  | DEFAULT 'ACTIVE' | ACTIVE, BANNED, SUSPENDED             |
| `avatar_url`         | TEXT         |                  | Link ảnh đại diện                     |
| `display_name`       | VARCHAR(100) |                  | Tên hiển thị                          |
| `bio`                | VARCHAR(500) |                  | Giới thiệu bản thân                   |
| `streak_count`       | INT          | DEFAULT 0        | Số ngày liên tục submit               |
| `streak_last_date`   | DATE         |                  | Ngày cuối cùng có submission approved |
| `login_failed_count` | INT          | DEFAULT 0        | Số lần login sai liên tiếp            |
| `locked_until`       | TIMESTAMP    |                  | Thời gian khóa tài khoản              |
| `created_at`         | TIMESTAMP    | DEFAULT NOW()    |                                       |
| `updated_at`         | TIMESTAMP    | DEFAULT NOW()    |                                       |

### 1.2. Bảng `challenges`

| Column             | Type         | Constraints           | Description                                |
| :----------------- | :----------- | :-------------------- | :----------------------------------------- |
| `id`               | UUID         | PRIMARY KEY           |                                            |
| `creator_id`       | UUID         | FK -> users.id        | Người tạo challenge                        |
| `title`            | VARCHAR(255) | NOT NULL              | Tiêu đề thử thách                          |
| `description`      | TEXT         |                       | Mô tả chi tiết                             |
| `status`           | VARCHAR(20)  | NOT NULL              | DRAFT, PUBLISHED, ONGOING, ENDED, ARCHIVED |
| `cover_url`        | TEXT         |                       | Ảnh bìa challenge                          |
| `start_date`       | TIMESTAMP    |                       | Ngày bắt đầu                               |
| `end_date`         | TIMESTAMP    |                       | Ngày kết thúc                              |
| `difficulty`       | VARCHAR(20)  |                       | EASY, MEDIUM, HARD                         |
| `max_participants` | INT          |                       | Số người tham gia tối đa (NULL = vô hạn)   |
| `allow_late_join`  | BOOLEAN      | DEFAULT TRUE          | Cho phép join khi đang ONGOING             |
| `task_unlock_mode` | VARCHAR(20)  | DEFAULT 'ALL_AT_ONCE' | ALL_AT_ONCE hoặc DAILY_UNLOCK              |
| `created_at`       | TIMESTAMP    | DEFAULT NOW()         |                                            |
| `updated_at`       | TIMESTAMP    | DEFAULT NOW()         |                                            |

### 1.3. Bảng `tasks`

| Column         | Type         | Constraints         | Description              |
| :------------- | :----------- | :------------------ | :----------------------- |
| `id`           | UUID         | PRIMARY KEY         |                          |
| `challenge_id` | UUID         | FK -> challenges.id | Thuộc challenge nào      |
| `title`        | VARCHAR(255) | NOT NULL            |                          |
| `content`      | TEXT         |                     | Nội dung/Yêu cầu task    |
| `day_number`   | INT          | NOT NULL            | Thứ tự ngày (1, 2, 3...) |
| `max_score`    | INT          | DEFAULT 10          | Điểm tối đa              |
| `created_at`   | TIMESTAMP    | DEFAULT NOW()       |                          |
| `updated_at`   | TIMESTAMP    | DEFAULT NOW()       |                          |

**Unique Constraint:** `(challenge_id, day_number)` — Mỗi challenge không được trùng day_number.

### 1.4. Bảng `user_challenges` (Bảng trung gian tham gia)

| Column         | Type        | Constraints         | Description                        |
| :------------- | :---------- | :------------------ | :--------------------------------- |
| `id`           | UUID        | PRIMARY KEY         |                                    |
| `user_id`      | UUID        | FK -> users.id      |                                    |
| `challenge_id` | UUID        | FK -> challenges.id |                                    |
| `status`       | VARCHAR(20) | NOT NULL            | ACTIVE, QUIT, DONE                 |
| `total_score`  | INT         | DEFAULT 0           | Tổng điểm tích lũy trong challenge |
| `joined_at`    | TIMESTAMP   | DEFAULT NOW()       |                                    |
| `updated_at`   | TIMESTAMP   | DEFAULT NOW()       |                                    |

**Unique Constraint:** `(user_id, challenge_id)` — Mỗi user chỉ join 1 lần cho 1 challenge.

### 1.5. _(Removed — Refresh Tokens lưu trong Redis, xem mục 3)_

### 1.6. Bảng `media` (PostgreSQL - Metadata)

Lưu trữ thông tin định danh của tệp tin đã được upload thành công.

| Column             | Type         | Constraints       | Description                      |
| :----------------- | :----------- | :---------------- | :------------------------------- |
| `id`               | UUID         | PRIMARY KEY       |                                  |
| `user_id`          | UUID         | FK -> users.id    | Chủ sở hữu tệp                   |
| `storage_provider` | VARCHAR(20)  | NOT NULL          | minio, cloudinary, r2            |
| `file_key`         | VARCHAR(500) | NOT NULL          | Key/path trên storage provider   |
| `file_url`         | TEXT         |                   | Public URL (set sau khi confirm) |
| `file_type`        | VARCHAR(50)  |                   | jpg, mp4, etc.                   |
| `file_size`        | BIGINT       |                   | Kích thước (bytes)               |
| `status`           | VARCHAR(20)  | DEFAULT 'PENDING' | PENDING, CONFIRMED               |
| `created_at`       | TIMESTAMP    | DEFAULT NOW()     |                                  |

### 1.7. Bảng `submissions`

| Column          | Type         | Constraints              | Description                          |
| :-------------- | :----------- | :----------------------- | :----------------------------------- |
| `id`            | UUID         | PRIMARY KEY              |                                      |
| `task_id`       | UUID         | FK -> tasks.id           |                                      |
| `user_id`       | UUID         | FK -> users.id           |                                      |
| `description`   | TEXT         |                          | Mô tả bài nộp của user               |
| `media_id`      | UUID         | FK -> media.id, NULLABLE | Liên kết tới metadata tệp (tùy chọn) |
| `status`        | VARCHAR(20)  | NOT NULL                 | PENDING, APPROVED, REJECTED          |
| `score`         | INT          |                          | Điểm đạt được (do Moderator chấm)    |
| `reviewer_id`   | UUID         | FK -> users.id, NULLABLE | Moderator đã duyệt bài               |
| `reviewed_at`   | TIMESTAMP    |                          | Thời điểm duyệt                      |
| `reject_reason` | VARCHAR(500) |                          | Lý do từ chối (khi REJECTED)         |
| `submitted_at`  | TIMESTAMP    | DEFAULT NOW()            |                                      |
| `updated_at`    | TIMESTAMP    | DEFAULT NOW()            |                                      |

**Unique Constraint:** `(task_id, user_id)` — Mỗi user chỉ có **1 bài nộp duy nhất** cho 1 task. Nếu bị REJECTED, user cập nhật (resubmit) bài nộp hiện tại thay vì tạo mới.

### 1.8. Bảng `badges`

Hệ thống huy hiệu (Gamification) — Ghi nhận thành tích đặc biệt của người dùng.

| Column        | Type         | Constraints      | Description                                |
| :------------ | :----------- | :--------------- | :----------------------------------------- |
| `id`          | UUID         | PRIMARY KEY      |                                            |
| `code`        | VARCHAR(50)  | UNIQUE, NOT NULL | Mã huy hiệu (FIRST_SUBMIT, STREAK_7, etc.) |
| `name`        | VARCHAR(100) | NOT NULL         | Tên hiển thị                               |
| `description` | VARCHAR(500) |                  | Mô tả điều kiện đạt được                   |
| `icon_url`    | TEXT         |                  | URL icon                                   |
| `created_at`  | TIMESTAMP    | DEFAULT NOW()    |                                            |

### 1.9. Bảng `user_badges`

| Column      | Type      | Constraints     | Description   |
| :---------- | :-------- | :-------------- | :------------ |
| `id`        | UUID      | PRIMARY KEY     |               |
| `user_id`   | UUID      | FK -> users.id  |               |
| `badge_id`  | UUID      | FK -> badges.id |               |
| `earned_at` | TIMESTAMP | DEFAULT NOW()   | Thời điểm đạt |

**Unique Constraint:** `(user_id, badge_id)` — Mỗi badge chỉ cấp 1 lần cho 1 user.

### 1.10. Bảng `submission_score_events`

Bảng idempotency cho side effect cộng điểm của `SubmissionApprovedEvent`.

| Column          | Type      | Constraints                    | Description                        |
| :-------------- | :-------- | :----------------------------- | :--------------------------------- |
| `id`            | UUID      | PRIMARY KEY                    |                                    |
| `submission_id` | UUID      | FK -> submissions.id, NOT NULL | Submission đã được xử lý cộng điểm |
| `created_at`    | TIMESTAMP | DEFAULT NOW()                  |                                    |
| `updated_at`    | TIMESTAMP | DEFAULT NOW()                  |                                    |

**Unique Constraint:** `(submission_id)` — mỗi submission chỉ được cộng điểm 1 lần.

---

## 2. MongoDB (Logs, Social, Notifications & Messaging)

Sử dụng để lưu trữ dữ liệu lớn, ít thay đổi hoặc cần truy vấn nhanh theo dạng Document.

### 2.1. Collection `audit_logs`

Lưu vết mọi hành động quản trị và thay đổi trạng thái quan trọng.

```json
{
  "_id": "ObjectId",
  "actor_id": "UUID",
  "actor_role": "String",
  "action": "APPROVE_SUBMISSION",
  "resource_type": "SUBMISSION",
  "resource_id": "UUID",
  "old_value": { "status": "PENDING" },
  "new_value": { "status": "APPROVED", "score": 10 },
  "ip_address": "String",
  "user_agent": "String",
  "timestamp": "ISODate"
}
```

### 2.2. Collection `activity_feed`

Dùng để hiển thị bảng tin (Social Feed) trên ứng dụng.

```json
{
  "_id": "ObjectId",
  "userId": "UUID",
  "type": "JOIN_CHALLENGE | COMPLETE_TASK | ...",
  "referenceId": "String",
  "createdAt": "ISODate",
  "message": "String|null",
  "metadata": { "anyKey": "anyValue" }
}
```

- Trường tối thiểu cho Phase C: `id`, `userId`, `type`, `referenceId`, `createdAt`.
- `message` và `metadata` là extension fields (optional, phục vụ mở rộng hiển thị).

### 2.3. Collection `comments`

```json
{
  "_id": "ObjectId",
  "submission_id": "UUID",
  "user_id": "UUID",
  "username": "String",
  "avatar_url": "String",
  "content": "String",
  "created_at": "ISODate",
  "updated_at": "ISODate"
}
```

### 2.4. Collection `reactions`

Dùng cơ chế **Upsert** (nếu đã tồn tại thì xóa/đổi, chưa có thì thêm).

```json
{
  "_id": "ObjectId",
  "submission_id": "UUID",
  "user_id": "UUID",
  "type": "LIKE | HEART | FIRE",
  "created_at": "ISODate"
}
```

**Unique Compound Index:** `{ submission_id: 1, user_id: 1 }` — Mỗi user chỉ react 1 lần cho 1 submission.

### 2.5. Collection `notifications`

Lưu trữ thông báo cho người dùng. Hỗ trợ đẩy real-time qua WebSocket.

```json
{
  "_id": "ObjectId",
  "userId": "UUID",
  "type": "SUBMISSION_APPROVED | NEW_PARTICIPANT | ...",
  "payload": {
    "title": "String",
    "message": "String",
    "metadata": { "anyKey": "anyValue" }
  },
  "referenceId": "String",
  "isRead": false,
  "createdAt": "ISODate"
}
```

- Trường lõi theo contract Phase C: `id`, `userId`, `type`, `payload`, `isRead`, `createdAt`.
- `payload` là JSON mở rộng, không đóng cứng schema theo từng loại thông báo.

**Indexes:**

- `{ userId: 1, isRead: 1, createdAt: -1 }` — Truy vấn nhanh thông báo theo user + trạng thái đọc.
- Unique partial index: `{ userId: 1, type: 1, referenceId: 1 }` với điều kiện `referenceId` tồn tại và không rỗng.
- TTL Index: `{ createdAt: 1 }, expireAfterSeconds: 7776000` — Tự xóa sau 90 ngày.

### 2.6. Collection `chat_conversations`

```json
{
  "_id": "ObjectId",
  "type": "DIRECT | CHALLENGE_CHANNEL",
  "challenge_id": "UUID|null",
  "channel_key": "String|null",
  "channel_name": "String|null",
  "is_default": false,
  "is_readonly": false,
  "created_by": "UUID",
  "participants_hash": "String|null",
  "last_message": {
    "message_id": "ObjectId",
    "sender_id": "UUID",
    "content_preview": "String",
    "sent_at": "ISODate"
  },
  "created_at": "ISODate",
  "updated_at": "ISODate",
  "archived": false
}
```

**Quy tắc dữ liệu:**

- `DIRECT`: `participants_hash` bắt buộc, tạo từ cặp user đã sort (đảm bảo unique A-B/B-A).
- `CHALLENGE_CHANNEL`: bắt buộc `challenge_id`, `channel_key`, `channel_name`.
- Mỗi challenge phải có channel mặc định `general`.

### 2.7. Collection `chat_memberships`

```json
{
  "_id": "ObjectId",
  "conversation_id": "ObjectId",
  "user_id": "UUID",
  "role": "OWNER | MEMBER | MODERATOR",
  "last_read_message_id": "ObjectId|null",
  "last_read_at": "ISODate|null",
  "unread_count": 0,
  "muted": false,
  "joined_at": "ISODate",
  "left_at": "ISODate|null",
  "updated_at": "ISODate"
}
```

**Unique Compound Index:** `{ conversation_id: 1, user_id: 1 }`.

### 2.8. Collection `chat_messages`

```json
{
  "_id": "ObjectId",
  "conversation_id": "ObjectId",
  "sender_id": "UUID",
  "sender_username": "String",
  "sender_avatar_url": "String|null",
  "type": "TEXT | MEDIA | SYSTEM",
  "content": "String",
  "attachments": [
    {
      "media_id": "UUID",
      "file_url": "String",
      "file_type": "String",
      "file_size": "NumberLong"
    }
  ],
  "edited_at": "ISODate|null",
  "deleted_at": "ISODate|null",
  "created_at": "ISODate"
}
```

**Quy tắc:**

- `deleted_at != null` nghĩa là soft-delete (giữ lại record để đồng bộ lịch sử conversation).
- Message history không dùng TTL ở MVP.

---

## 3. Redis Data Structures

Redis lưu trữ Refresh Token Family, Access Token Blacklist, Leaderboard, và Cache.

### 3.1. Refresh Token Family (RT Rotation + Replay Detection)

Toàn bộ cơ chế Refresh Token được lưu trong Redis thay vì Postgres. TTL tự động xóa token hết hạn — không cần Scheduled Job cleanup.

#### 3.1.1. RT Lookup (theo SHA-256 hash)

```text
Key:     rt:<token_hash>
Type:    HASH
Fields:  { user_id, family_id, revoked }   # revoked = "0" | "1"
TTL:     604800 (7 ngày)
```

- Mỗi Refresh Token được hash SHA-256 trước khi lưu.
- Khi rotate: set `revoked = "1"` trên RT cũ, tạo RT mới cùng `family_id`.
- Khi RT hết hạn, Redis tự xóa key (TTL).

#### 3.1.2. Family Metadata

```text
Key:     rt_family:<family_id>
Type:    HASH
Fields:  { user_id, blocked }              # blocked = "0" | "1"
TTL:     604800 (7 ngày)
```

- Mỗi lần login = 1 `family_id` mới (UUID).
- Khi phát hiện replay → `blocked = "1"`.

#### 3.1.3. Family → Token Hashes (bulk operations)

```text
Key:     rt_family_tokens:<family_id>
Type:    SET
Members: [ token_hash_1, token_hash_2, ... ]
TTL:     604800 (7 ngày)
```

- Dùng cho logout (revoke toàn bộ family) và block family.

#### 3.1.4. User → Families (cho ban/suspend)

```text
Key:     rt_user_families:<user_id>
Type:    SET
Members: [ family_id_1, family_id_2, ... ]
TTL:     604800 (7 ngày, refresh khi login mới)
```

- Dùng khi Admin ban/suspend user → lấy tất cả families để block + revoke.

#### Quy tắc Refresh Token Family:

1. **Login:** Tạo RT mới → hash SHA-256 → lưu `rt:<hash>`, tạo `rt_family:<family_id>`, SADD vào `rt_family_tokens:<family_id>` và `rt_user_families:<user_id>`. Set TTL 7 ngày cho tất cả keys.
2. **Refresh (rotate):** Lookup `rt:<hash>` → check `revoked` and `rt_family:<family_id>.blocked`. Nếu OK: set `revoked = "1"` trên RT cũ, tạo RT mới cùng `family_id`.
3. **Replay Detection:** Nếu `rt:<hash>.revoked = "1"` → token đã bị rotate → **token replay attack**. Set `rt_family:<family_id>.blocked = "1"`, revoke tất cả RT trong family, blacklist AT hiện tại.
4. **Logout:** Set `rt_family:<family_id>.blocked = "1"`, xóa tất cả `rt:<hash>` trong family, SREM family khỏi `rt_user_families:<user_id>`, blacklist AT (`jti`) vào Redis.
5. **Ban/Suspend:** Lấy tất cả families từ `rt_user_families:<user_id>` → block từng family → xóa tất cả RT keys → xóa `rt_user_families:<user_id>`.
6. **Validation:** RT hợp lệ khi `rt:<hash>` tồn tại AND `revoked = "0"` AND `rt_family:<family_id>.blocked = "0"`.

### 3.2. Access Token Blacklist

```text
Key:     blacklist:at:<jti>       # jti = JWT ID (unique identifier của Access Token)
Type:    STRING
Value:   "1"
TTL:     <remaining_AT_lifetime>  # Tự động xóa khi AT hết hạn
```

- Mỗi request cần kiểm tra AT có nằm trong Redis blacklist không trước khi xử lý.
- Blacklist AT khi: logout, replay detection (block family), ban/suspend user.

### 3.3. Leaderboard (ZSET)

```text
Key:     leaderboard:<challenge_id>
Member:  <user_id>
Score:   <total_score>
```

- Cập nhật khi submission được APPROVED: `ZINCRBY leaderboard:<challenge_id> <score> <user_id>`
- Lấy top N: `ZREVRANGE leaderboard:<challenge_id> 0 N-1 WITHSCORES`
- Lấy rank: `ZREVRANK leaderboard:<challenge_id> <user_id>`

### 3.4. Cache (Optional)

```text
Key:     cache:challenge:<id>
Value:   JSON string (challenge detail)
TTL:     300 (5 phút)
```

### 3.5. Chat Presence & Unread Cache (Optional)

```text
Key:     presence:user:<user_id>
Type:    STRING
Value:   "online"
TTL:     60
```

- FE heartbeat định kỳ để refresh TTL và hiển thị trạng thái online/offline cho DM.

```text
Key:     chat:unread:total:<user_id>
Type:    STRING
Value:   <count>
TTL:     300
```

- Cache nhanh số lượng unread tổng; khi lệch dữ liệu phải fallback query MongoDB `chat_memberships`.

---

## 4. Chỉ dẫn Implementation (JPA/Spring Data)

- **Naming Strategy:** Postgres dùng `snake_case` ở mức physical table/column; API contract và Mongo document dùng `camelCase`.
- **Auditing:** Sử dụng `@CreatedDate`, `@LastModifiedDate` của Spring Data JPA.
- **Soft Delete:** Hiện tại không dùng soft delete. Nếu cần trong tương lai, thêm `deleted_at` TIMESTAMP NULLABLE.

### 4.1. Indexes (Postgres)

| Bảng                      | Cột                          | Loại Index |
| :------------------------ | :--------------------------- | :--------- |
| `users`                   | `email`, `username`          | UNIQUE     |
| `users`                   | `status`                     | B-TREE     |
| `challenges`              | `creator_id`                 | B-TREE     |
| `challenges`              | `status`                     | B-TREE     |
| `tasks`                   | `challenge_id`               | B-TREE     |
| `tasks`                   | `(challenge_id, day_number)` | UNIQUE     |
| `user_challenges`         | `(user_id, challenge_id)`    | UNIQUE     |
| `user_challenges`         | `challenge_id`               | B-TREE     |
| `submissions`             | `(task_id, user_id)`         | UNIQUE     |
| `submissions`             | `user_id`                    | B-TREE     |
| `submissions`             | `status`                     | B-TREE     |
| `submission_score_events` | `submission_id`              | UNIQUE     |
| `user_badges`             | `(user_id, badge_id)`        | UNIQUE     |

### 4.2. Indexes (MongoDB)

| Collection           | Fields                                          | Loại                                    |
| :------------------- | :---------------------------------------------- | :-------------------------------------- |
| `audit_logs`         | `{ actor_id: 1, timestamp: -1 }`                | Compound                                |
| `audit_logs`         | `{ resource_type: 1, resource_id: 1 }`          | Compound                                |
| `activity_feed`      | `{ userId: 1, createdAt: -1 }`                  | Compound                                |
| `activity_feed`      | `{ userId: 1, type: 1, referenceId: 1 }`        | Unique (partial: referenceId non-empty) |
| `comments`           | `{ submission_id: 1, created_at: -1 }`          | Compound                                |
| `reactions`          | `{ submission_id: 1, user_id: 1 }`              | Unique                                  |
| `notifications`      | `{ userId: 1, isRead: 1, createdAt: -1 }`       | Compound                                |
| `notifications`      | `{ userId: 1, type: 1, referenceId: 1 }`        | Unique (partial: referenceId non-empty) |
| `notifications`      | `{ createdAt: 1 }, expireAfterSeconds: 7776000` | TTL                                     |
| `chat_conversations` | `{ type: 1, updated_at: -1 }`                   | Compound                                |
| `chat_conversations` | `{ participants_hash: 1 }`                      | Unique (partial cho DIRECT)             |
| `chat_conversations` | `{ challenge_id: 1, channel_key: 1 }`           | Unique (partial cho CHALLENGE_CHANNEL)  |
| `chat_memberships`   | `{ conversation_id: 1, user_id: 1 }`            | Unique                                  |
| `chat_memberships`   | `{ user_id: 1, updated_at: -1 }`                | Compound                                |
| `chat_messages`      | `{ conversation_id: 1, created_at: -1 }`        | Compound                                |
| `chat_messages`      | `{ sender_id: 1, created_at: -1 }`              | Compound                                |
