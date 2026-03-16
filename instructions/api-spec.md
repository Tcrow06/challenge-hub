<!-- instructions/api-spec.md -->

# ChallengeHub: API Specification (v1)

Tài liệu này định nghĩa các Endpoint cho hệ thống. Tất cả API đều bắt đầu bằng prefix `/api/v1`.

---

## 1. Quy chuẩn chung

### 1.1. Request Format

- **Content-Type:** `application/json`
- **Xác thực:** Header `Authorization: Bearer <JWT_TOKEN>`
- **Timezone:** Mọi timestamp dùng **UTC** (ISO 8601: `2026-03-13T10:27:12Z`).

### 1.2. Pagination (Áp dụng cho tất cả GET list)

**Query Parameters:**

| Param  | Type   | Default | Max | Mô tả                       |
| :----- | :----- | :------ | :-- | :-------------------------- |
| `page` | int    | 1       | —   | Số trang (1-based)          |
| `size` | int    | 10      | 50  | Số record mỗi trang         |
| `sort` | string | —       | —   | Sắp xếp: `field,asc\|desc`  |
| `q`    | string | —       | —   | Tìm kiếm full-text (nếu có) |

Ví dụ: `GET /challenges?page=2&size=20&sort=created_at,desc&q=javascript`

### 1.3. Standard Response Wrapper

Mọi API trả về tuân thủ cấu trúc thống nhất:

**Success Response:**

```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": { ... },
  "metadata": {
    "page": 1,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10
  },
  "timestamp": "2026-03-13T10:27:12Z"
}
```

**Error Response:**

```json
{
  "success": false,
  "message": "Mô tả lỗi cho người dùng",
  "errorCode": "CHALLENGE_NOT_FOUND",
  "errors": [{ "field": "email", "message": "Email không hợp lệ" }],
  "data": null,
  "metadata": null,
  "timestamp": "2026-03-13T10:27:12Z"
}
```

- `errorCode`: Tham chiếu Error Code Catalog trong `requirements.md` mục 8.
- `errors`: Chỉ xuất hiện khi `errorCode = VALIDATION_FAILED` (field-level validation errors).

**Quy ước field trong mọi response:**

- `success`: luôn có, boolean.
- `message`: luôn có, mô tả ngắn gọn cho người dùng.
- `data`:
  - Success: chứa payload chính (object/array/null).
  - Error: luôn là `null`.
- `metadata`:
  - Success: dùng cho thông tin phân trang hoặc meta khác (có thể `null` nếu không cần).
  - Error: luôn là `null`.
- `errorCode`:
  - Chỉ có khi `success = false`.
- `errors`:
  - Chỉ có khi `success = false` và `errorCode = VALIDATION_FAILED`.

### 1.4. HTTP Status Codes

| Code | Ý nghĩa                                             |
| :--- | :-------------------------------------------------- |
| 200  | Thành công (GET, PUT, PATCH, DELETE)                |
| 201  | Tạo mới thành công (POST)                           |
| 400  | Dữ liệu gửi lên không hợp lệ                        |
| 401  | Chưa đăng nhập hoặc Token hết hạn                   |
| 403  | Không có quyền truy cập tài nguyên này              |
| 404  | Không tìm thấy tài nguyên                           |
| 409  | Conflict (duplicate resource, already joined, etc.) |
| 423  | Tài khoản bị khóa (Account Locked)                  |
| 429  | Rate limit exceeded                                 |
| 500  | Lỗi hệ thống                                        |

---

## 2. Authentication API (`/api/v1/auth`)

### POST `/register`

Đăng ký tài khoản mới. **Access: Public**

**Request Body:**

```json
{
  "username": "string (3-50 chars, alphanumeric + underscore)",
  "email": "string (valid email, max 100)",
  "password": "string (8-128 chars, 1 uppercase, 1 lowercase, 1 digit)"
}
```

**Response (201):**

```json
{
  "data": {
    "id": "uuid",
    "username": "string",
    "email": "string",
    "role": "USER",
    "created_at": "timestamp"
  }
}
```

**Errors:** `VALIDATION_FAILED`, `VALIDATION_DUPLICATE_EMAIL`, `VALIDATION_DUPLICATE_USERNAME`

---

### POST `/login`

Đăng nhập và nhận JWT + Refresh Token. **Access: Public**

**Request Body:**

```json
{
  "email": "string",
  "password": "string"
}
```

**Response (200):**

```json
{
  "data": {
    "access_token": "jwt_string",
    "token_type": "Bearer",
    "expires_in": 900,
    "user": {
      "id": "uuid",
      "username": "string",
      "email": "string",
      "role": "USER",
      "avatar_url": "string|null",
      "display_name": "string|null"
    }
  }
}
```

- Refresh Token được set vào **HttpOnly Secure Cookie** (`ch_refresh_token`), không trả về trong body.

**Errors:** `AUTH_INVALID_CREDENTIALS`, `AUTH_ACCOUNT_LOCKED`, `AUTH_ACCOUNT_BANNED`, `AUTH_ACCOUNT_SUSPENDED`

---

### POST `/refresh`

Lấy Access Token mới từ Refresh Token (Rotate RT). **Access: Public** (RT trong cookie)

**Request:** Không có body. RT được đọc từ cookie `ch_refresh_token`.

**Response (200):**

```json
{
  "data": {
    "access_token": "jwt_string",
    "token_type": "Bearer",
    "expires_in": 900
  }
}
```

- RT mới được set lại vào cookie.

**Errors:** `AUTH_TOKEN_INVALID`, `AUTH_REFRESH_REPLAY`, `AUTH_TOKEN_EXPIRED`

---

### POST `/logout`

Đăng xuất: block RT family + blacklist AT. **Access: USER**

**Request:** Không có body. AT trong header, RT trong cookie.

**Response (200):**

```json
{
  "data": null,
  "message": "Đăng xuất thành công"
}
```

---

## 3. User & Profile API (`/api/v1/users`)

### GET `/me`

Lấy profile của user hiện tại. **Access: USER**

**Response (200):**

```json
{
  "data": {
    "id": "uuid",
    "username": "string",
    "email": "string",
    "role": "USER",
    "display_name": "string|null",
    "bio": "string|null",
    "avatar_url": "string|null",
    "streak_count": 7,
    "created_at": "timestamp",
    "badges": [
      {
        "code": "STREAK_7",
        "name": "Chiến binh tuần lễ",
        "icon_url": "url",
        "earned_at": "timestamp"
      }
    ],
    "stats": {
      "challenges_joined": 5,
      "challenges_completed": 2,
      "total_submissions": 30,
      "total_score": 250
    }
  }
}
```

---

### PUT `/me`

Cập nhật profile cá nhân. **Access: USER**

**Request Body:**

```json
{
  "display_name": "string|null (max 100)",
  "bio": "string|null (max 500)",
  "avatar_url": "string|null (URL from media upload)"
}
```

**Response (200):** Trả về profile đã cập nhật (cùng format GET /me).

**Errors:** `VALIDATION_FAILED`

---

### GET `/{id}/stats`

Xem public profile của user khác. **Access: Public**

**Response (200):**

```json
{
  "data": {
    "id": "uuid",
    "username": "string",
    "display_name": "string|null",
    "avatar_url": "string|null",
    "streak_count": 7,
    "badges": [ ... ],
    "stats": {
      "challenges_joined": 5,
      "challenges_completed": 2,
      "total_score": 250
    }
  }
}
```

---

## 4. Challenge API (`/api/v1/challenges`)

### GET `/`

Lấy danh sách Challenge (phân trang + filter). **Access: Public**

**Query Parameters (ngoài pagination):**

| Param        | Type   | Mô tả                            |
| :----------- | :----- | :------------------------------- |
| `status`     | string | Filter: PUBLISHED, ONGOING, etc. |
| `difficulty` | string | Filter: EASY, MEDIUM, HARD       |
| `creator_id` | uuid   | Filter theo người tạo            |

**Response (200):**

```json
{
  "data": [
    {
      "id": "uuid",
      "title": "string",
      "description": "string (truncated 200 chars)",
      "status": "PUBLISHED",
      "difficulty": "MEDIUM",
      "cover_url": "string|null",
      "start_date": "timestamp",
      "end_date": "timestamp",
      "task_count": 30,
      "participant_count": 120,
      "creator": {
        "id": "uuid",
        "username": "string",
        "avatar_url": "string|null"
      },
      "created_at": "timestamp"
    }
  ],
  "metadata": { "page": 1, "size": 10, "totalElements": 50, "totalPages": 5 }
}
```

---

### GET `/{id}`

Xem chi tiết 1 Challenge. **Access: Public**

**Response (200):**

```json
{
  "data": {
    "id": "uuid",
    "title": "string",
    "description": "string (full)",
    "status": "ONGOING",
    "difficulty": "MEDIUM",
    "cover_url": "string|null",
    "start_date": "timestamp",
    "end_date": "timestamp",
    "max_participants": 100,
    "allow_late_join": true,
    "task_unlock_mode": "DAILY_UNLOCK",
    "task_count": 30,
    "participant_count": 85,
    "creator": {
      "id": "uuid",
      "username": "string",
      "avatar_url": "string|null"
    },
    "is_joined": false,
    "created_at": "timestamp",
    "updated_at": "timestamp"
  }
}
```

- `is_joined`: `true` nếu user hiện tại đã join (NULL nếu chưa đăng nhập).

**Errors:** `CHALLENGE_NOT_FOUND`

---

### POST `/`

Tạo Challenge mới (DRAFT). **Access: CREATOR**

**Request Body:**

```json
{
  "title": "string (1-255 chars, required)",
  "description": "string (optional)",
  "difficulty": "EASY|MEDIUM|HARD (optional)",
  "cover_url": "string|null",
  "start_date": "timestamp (optional — required trước khi publish)",
  "end_date": "timestamp (optional — required trước khi publish)",
  "max_participants": "int|null",
  "allow_late_join": true,
  "task_unlock_mode": "ALL_AT_ONCE|DAILY_UNLOCK"
}
```

**Response (201):** Trả về challenge đã tạo (format GET /{id}).

**Errors:** `VALIDATION_FAILED`, `FORBIDDEN`

---

### PUT `/{id}`

Cập nhật thông tin Challenge. **Access: CREATOR (owner only)**

Chỉ cho phép khi status = `DRAFT`. Request body giống POST.

**Errors:** `CHALLENGE_NOT_FOUND`, `FORBIDDEN`, `CHALLENGE_INVALID_TRANSITION`

---

### PATCH `/{id}/status`

Chuyển trạng thái Challenge (State Machine). **Access: CREATOR (owner) / ADMIN**

**Request Body:**

```json
{
  "status": "PUBLISHED|DRAFT|ARCHIVED"
}
```

- Transition rules theo State Machine trong `requirements.md` mục 3.1.
- ONGOING và ENDED chỉ được chuyển bởi **Scheduled Job** (không qua API).

**Errors:** `CHALLENGE_NOT_FOUND`, `CHALLENGE_INVALID_TRANSITION`, `CHALLENGE_HAS_PARTICIPANTS`, `CHALLENGE_MISSING_TASKS`, `CHALLENGE_MISSING_DATES`

---

### POST `/{id}/join`

Tham gia vào Challenge. **Access: USER**

**Request:** Không có body.

**Response (201):**

```json
{
  "data": {
    "challenge_id": "uuid",
    "user_id": "uuid",
    "status": "ACTIVE",
    "joined_at": "timestamp"
  }
}
```

**Errors:** `CHALLENGE_NOT_FOUND`, `CHALLENGE_NOT_JOINABLE`, `CHALLENGE_ALREADY_JOINED`, `CHALLENGE_FULL`

---

### POST `/{id}/quit`

Rời khỏi Challenge. **Access: USER (đã join, status ACTIVE)**

**Request:** Không có body.

**Response (200):**

```json
{
  "data": {
    "challenge_id": "uuid",
    "user_id": "uuid",
    "status": "QUIT",
    "quit_at": "timestamp"
  }
}
```

**Errors:** `CHALLENGE_NOT_FOUND`, `CHALLENGE_NOT_JOINED`, `CHALLENGE_ALREADY_QUIT`

---

### DELETE `/{id}`

Xóa Challenge. **Access: CREATOR (owner) / ADMIN**

Chỉ cho phép khi status = `DRAFT`.

**Errors:** `CHALLENGE_NOT_FOUND`, `FORBIDDEN`, `CHALLENGE_INVALID_TRANSITION`

---

## 5. Task API (`/api/v1/challenges/{challengeId}/tasks`)

### GET `/`

Lấy danh sách Task của Challenge. **Access: USER (đã join) hoặc Public (nếu PUBLISHED/ONGOING)**

**Response (200):**

```json
{
  "data": [
    {
      "id": "uuid",
      "day_number": 1,
      "title": "string",
      "content": "string",
      "max_score": 10,
      "is_unlocked": true,
      "my_submission": {
        "id": "uuid",
        "status": "APPROVED",
        "score": 8
      }
    }
  ]
}
```

- `is_unlocked`: Dựa vào `task_unlock_mode` và ngày hiện tại.
- `my_submission`: submission của user hiện tại (null nếu chưa submit hoặc chưa đăng nhập).

---

### POST `/`

Tạo Task cho Challenge. **Access: CREATOR (owner)**

Chỉ cho phép khi challenge status = `DRAFT`.

**Request Body:**

```json
{
  "title": "string (1-255 chars, required)",
  "content": "string (optional)",
  "day_number": "int (required, >= 1)",
  "max_score": "int (default 10, 1-100)"
}
```

**Response (201):** Task object.

**Errors:** `CHALLENGE_NOT_FOUND`, `FORBIDDEN`, `VALIDATION_FAILED`

---

### PUT `/{taskId}`

Cập nhật Task. **Access: CREATOR (owner)**

Chỉ cho phép khi challenge status = `DRAFT`.

---

### DELETE `/{taskId}`

Xóa Task. **Access: CREATOR (owner)**

Chỉ cho phép khi challenge status = `DRAFT`.

---

## 6. Submission API

Base path: các endpoint submit nằm dưới `/api/v1` (không có base path chung vì path khác nhau).

### POST `/api/v1/submissions/tasks/{taskId}/submit`

Nộp bài cho một Task. **Access: USER (đã join challenge)**

**Request Body:**

```json
{
  "description": "string (optional, max 2000 chars)",
  "media_id": "uuid|null (optional — ID từ media upload)"
}
```

**Response (201):**

```json
{
  "data": {
    "id": "uuid",
    "task_id": "uuid",
    "description": "string",
    "media": {
      "id": "uuid",
      "file_url": "string",
      "file_type": "jpg"
    },
    "status": "PENDING",
    "submitted_at": "timestamp"
  }
}
```

**Errors:** `TASK_NOT_FOUND`, `TASK_NOT_UNLOCKED`, `SUBMISSION_NOT_PARTICIPANT`, `SUBMISSION_ALREADY_EXISTS`, `SUBMISSION_CHALLENGE_ENDED`

---

### PUT `/api/v1/submissions/{id}`

Resubmit (cập nhật bài nộp bị REJECTED). **Access: USER (owner)**

**Preconditions:** `status = REJECTED` AND challenge đang `ONGOING`.

**Request Body:** Giống POST submit.

**Response (200):** Submission object (status reset về PENDING).

**Errors:** `SUBMISSION_NOT_FOUND`, `FORBIDDEN`, `SUBMISSION_ALREADY_APPROVED`, `SUBMISSION_INVALID_RESUBMIT`, `SUBMISSION_CHALLENGE_ENDED`

---

### GET `/api/v1/submissions/me`

Xem lịch sử bài nộp của bản thân. **Access: USER**

**Query Parameters:** Pagination + `challenge_id` (optional filter), `status` (optional filter).

**Response (200):**

```json
{
  "data": [
    {
      "id": "uuid",
      "task": { "id": "uuid", "title": "string", "day_number": 1, "max_score": 10 },
      "challenge": { "id": "uuid", "title": "string" },
      "description": "string",
      "media": { "id": "uuid", "file_url": "string", "file_type": "jpg" },
      "status": "APPROVED",
      "score": 8,
      "reject_reason": null,
      "submitted_at": "timestamp",
      "reviewed_at": "timestamp"
    }
  ],
  "metadata": { ... }
}
```

---

### PATCH `/api/v1/submissions/{id}/status`

Duyệt/Từ chối bài nộp. **Access: MODERATOR / ADMIN**

**Request Body (Approve):**

```json
{
  "status": "APPROVED",
  "score": 8
}
```

**Request Body (Reject):**

```json
{
  "status": "REJECTED",
  "reject_reason": "string (required, 1-500 chars)"
}
```

**Validation:**

- `score` bắt buộc khi APPROVED, phải `0 ≤ score ≤ task.max_score`.
- `reject_reason` bắt buộc khi REJECTED.

**Response (200):** Submission object đã cập nhật.

**Errors:** `SUBMISSION_NOT_FOUND`, `FORBIDDEN`, `SUBMISSION_SCORE_EXCEEDED`, `VALIDATION_FAILED`

---

### GET `/api/v1/submissions/{id}`

Xem chi tiết 1 bài nộp. **Access: USER (owner) / MODERATOR / ADMIN**

**Response (200):** Submission object (format giống item trong /submissions/me).

**Errors:** `SUBMISSION_NOT_FOUND`, `FORBIDDEN`

---

### GET `/api/v1/submissions/pending`

Lấy danh sách bài nộp chờ duyệt. **Access: MODERATOR / ADMIN**

**Query Parameters:** Pagination + `challenge_id` (optional filter).

**Response (200):** Danh sách submissions (format giống /submissions/me) với `status = PENDING`.

---

## 7. Leaderboard & Feed API (`/api/v1/social`)

### GET `/leaderboard/{challengeId}`

Lấy bảng xếp hạng. **Access: Public**

**Query Parameters:**

| Param | Type | Default | Mô tả          |
| :---- | :--- | :------ | :------------- |
| `top` | int  | 50      | Số lượng top N |

**Response (200):**

```json
{
  "data": {
    "challenge_id": "uuid",
    "rankings": [
      {
        "rank": 1,
        "user": {
          "id": "uuid",
          "username": "string",
          "avatar_url": "string|null"
        },
        "total_score": 95,
        "tasks_completed": 10
      }
    ],
    "my_rank": {
      "rank": 15,
      "total_score": 72,
      "tasks_completed": 8
    }
  }
}
```

- `my_rank`: Vị trí của user hiện tại (null nếu chưa đăng nhập hoặc chưa join).

---

### GET `/feed`

Lấy Activity Feed. **Access: USER**

**Query Parameters:** Pagination (`page`, `size`).

**Response (200):**

```json
{
  "data": [
    {
      "id": "objectid",
      "type": "COMPLETE_TASK",
      "message": "User A đã hoàn thành ngày 5 của Challenge X",
      "user": { "id": "uuid", "username": "string", "avatar_url": "string|null" },
      "metadata": { "challenge_id": "uuid", "task_id": "uuid" },
      "created_at": "timestamp"
    }
  ],
  "metadata": { ... }
}
```

---

## 8. Interaction API (`/api/v1/interactions`)

### POST `/submissions/{id}/comments`

Gửi bình luận. **Access: USER**

**Request Body:**

```json
{
  "content": "string (1-1000 chars, required)"
}
```

**Response (201):**

```json
{
  "data": {
    "id": "objectid",
    "content": "string",
    "user": { "id": "uuid", "username": "string", "avatar_url": "string|null" },
    "created_at": "timestamp"
  }
}
```

---

### GET `/submissions/{id}/comments`

Lấy danh sách bình luận. **Access: Public**

**Query Parameters:** Pagination.

---

### POST `/submissions/{id}/react`

Toggle cảm xúc. **Access: USER**

**Request Body:**

```json
{
  "type": "LIKE|HEART|FIRE"
}
```

- Nếu đã react cùng type → **xóa** (toggle off).
- Nếu đã react khác type → **đổi** type.
- Nếu chưa react → **thêm**.

**Response (200):**

```json
{
  "data": {
    "reacted": true,
    "type": "FIRE",
    "reaction_counts": { "LIKE": 5, "HEART": 3, "FIRE": 12 }
  }
}
```

---

### DELETE `/comments/{commentId}`

Xóa bình luận. **Access: USER (ownership) / ADMIN**

---

## 9. Notification API (`/api/v1/notifications`)

### GET `/`

Lấy danh sách thông báo. **Access: USER**

**Query Parameters:** Pagination + `unread_only` (boolean, default false).

**Response (200):**

```json
{
  "data": [
    {
      "id": "objectid",
      "type": "SUBMISSION_APPROVED",
      "title": "Bài nộp đã được duyệt",
      "message": "Bài nộp ngày 5 của Challenge X đạt 8/10 điểm",
      "metadata": { "submission_id": "uuid", "challenge_id": "uuid" },
      "read": false,
      "created_at": "timestamp"
    }
  ],
  "metadata": { ... }
}
```

- `type` có thể bao gồm: `SUBMISSION_APPROVED`, `SUBMISSION_REJECTED`, `NEW_COMMENT`, `NEW_REACTION`, `RANK_CHANGE`, `BADGE_EARNED`, `STREAK_WARNING`, `NEW_PARTICIPANT`, `PARTICIPANT_QUIT`, `NEW_CHAT_MESSAGE`.

---

### GET `/unread-count`

Lấy số lượng thông báo chưa đọc. **Access: USER**

**Response (200):**

```json
{
  "data": { "count": 5 }
}
```

---

### PATCH `/{id}/read`

Đánh dấu 1 thông báo đã đọc. **Access: USER (ownership)**

---

### PATCH `/read-all`

Đánh dấu tất cả thông báo đã đọc. **Access: USER**

---

## 10. Media API (`/api/v1/media`)

### POST `/upload-url`

Lấy signed URL để upload trực tiếp. **Access: USER**

**Request Body:**

```json
{
  "file_name": "string (required)",
  "content_type": "string (required — image/jpeg, image/png, image/webp, video/mp4)",
  "file_size": "long (required, bytes)"
}
```

**Validation:**

- Image: max 10MB (10485760 bytes).
- Video: max 200MB (209715200 bytes).
- Formats: `.jpg`, `.png`, `.webp`, `.mp4` only.

**Response (200):**

```json
{
  "data": {
    "media_id": "uuid",
    "upload_url": "string (signed URL)",
    "file_key": "string",
    "expires_in": 300
  }
}
```

**Errors:** `MEDIA_TOO_LARGE`, `MEDIA_INVALID_TYPE`, `MEDIA_UPLOAD_FAILED`

---

### POST `/confirm/{mediaId}`

Xác nhận upload thành công. **Access: USER (owner)**

Frontend gọi sau khi upload tệp lên Storage Provider thành công.

**Response (200):**

```json
{
  "data": {
    "id": "uuid",
    "file_url": "string (public URL)",
    "file_type": "jpg",
    "file_size": 1048576
  }
}
```

---

### DELETE `/{id}`

Xóa media. **Access: USER (ownership) / ADMIN**

---

## 11. Admin API (`/api/v1/admin`)

### GET `/users`

Lấy danh sách users. **Access: ADMIN**

**Query Parameters:** Pagination + `role` (filter), `status` (filter), `q` (search username/email).

**Response (200):** Danh sách users (id, username, email, role, status, created_at).

---

### PATCH `/users/{id}/role`

Thay đổi role user. **Access: ADMIN**

**Request Body:**

```json
{
  "role": "USER|CREATOR|MODERATOR|ADMIN"
}
```

---

### PATCH `/users/{id}/status`

Ban/Suspend/Activate user. **Access: ADMIN**

**Request Body:**

```json
{
  "status": "ACTIVE|BANNED|SUSPENDED",
  "reason": "string (required when BANNED/SUSPENDED)"
}
```

- Khi BAN/SUSPEND: Lấy tất cả families từ Redis `rt_user_families:<user_id>` → block từng family → xóa tất cả RT keys.

---

### GET `/audit-logs`

Xem audit logs. **Access: ADMIN**

**Query Parameters:** Pagination + `action` (filter), `actor_id` (filter), `resource_type` (filter), `from` / `to` (date range).

---

## 12. Messaging API (`/api/v1/chat`)

### GET `/conversations`

Lấy danh sách inbox hội thoại của user (DM + Challenge Channels). **Access: USER**

**Query Parameters:** Pagination + các filter sau:

| Param          | Type   | Mặc định | Mô tả                                          |
| :------------- | :----- | :------- | :--------------------------------------------- |
| `type`         | string | —        | `DIRECT` hoặc `CHALLENGE_CHANNEL`              |
| `challenge_id` | uuid   | —        | Chỉ lấy hội thoại thuộc challenge cụ thể       |
| `q`            | string | —        | Tìm theo tên channel hoặc username counterpart |

**Response (200):**

```json
{
  "data": [
    {
      "id": "objectid",
      "type": "DIRECT",
      "counterpart": {
        "id": "uuid",
        "username": "string",
        "avatar_url": "string|null"
      },
      "challenge": null,
      "channel": null,
      "last_message": {
        "id": "objectid",
        "sender_id": "uuid",
        "content_preview": "Chào bạn",
        "sent_at": "timestamp"
      },
      "unread_count": 3,
      "updated_at": "timestamp"
    },
    {
      "id": "objectid",
      "type": "CHALLENGE_CHANNEL",
      "counterpart": null,
      "challenge": {
        "id": "uuid",
        "title": "30 Day Java Challenge"
      },
      "channel": {
        "key": "general",
        "name": "General",
        "is_default": true
      },
      "last_message": {
        "id": "objectid",
        "sender_id": "uuid",
        "content_preview": "Mọi người đến task ngày 5 chưa?",
        "sent_at": "timestamp"
      },
      "unread_count": 0,
      "updated_at": "timestamp"
    }
  ],
  "metadata": { "page": 1, "size": 10, "totalElements": 25, "totalPages": 3 }
}
```

---

### GET `/unread-count`

Lấy tổng số tin nhắn chưa đọc của toàn bộ chat. **Access: USER**

**Response (200):**

```json
{
  "data": { "count": 12 }
}
```

---

### POST `/direct/{targetUserId}`

Mở hoặc tạo cuộc trò chuyện riêng giữa 2 user. **Access: USER**

**Request:** Không body.

**Response (200):**

```json
{
  "data": {
    "conversation_id": "objectid",
    "type": "DIRECT",
    "counterpart": {
      "id": "uuid",
      "username": "string",
      "avatar_url": "string|null"
    },
    "created": true
  }
}
```

- Nếu conversation đã tồn tại, trả về `created = false`.

**Errors:** `CHAT_DM_SELF_NOT_ALLOWED`, `NOT_FOUND`, `FORBIDDEN`

---

### GET `/challenges/{challengeId}/channels`

Lấy danh sách channel chat của một challenge. **Access: USER (participant) / CREATOR (owner) / MODERATOR / ADMIN**

**Response (200):**

```json
{
  "data": [
    {
      "conversation_id": "objectid",
      "channel_key": "general",
      "name": "General",
      "is_default": true,
      "is_readonly": false,
      "member_count": 128
    },
    {
      "conversation_id": "objectid",
      "channel_key": "qna",
      "name": "Q&A",
      "is_default": false,
      "is_readonly": false,
      "member_count": 128
    }
  ]
}
```

**Errors:** `CHALLENGE_NOT_FOUND`, `CHAT_MEMBER_REQUIRED`, `FORBIDDEN`

---

### POST `/challenges/{challengeId}/channels`

Tạo channel mới trong challenge (kiểu Discord channel). **Access: CREATOR (owner) / MODERATOR / ADMIN**

**Request Body:**

```json
{
  "channel_key": "qna",
  "name": "Q&A",
  "is_readonly": false
}
```

**Validation:**

- `channel_key`: `^[a-z0-9-]{2,30}$`
- `name`: 2..50 chars

**Response (201):**

```json
{
  "data": {
    "conversation_id": "objectid",
    "challenge_id": "uuid",
    "channel_key": "qna",
    "name": "Q&A",
    "is_default": false,
    "is_readonly": false,
    "created_at": "timestamp"
  }
}
```

**Errors:** `CHALLENGE_NOT_FOUND`, `CHAT_CHANNEL_ALREADY_EXISTS`, `FORBIDDEN`, `VALIDATION_FAILED`

---

### GET `/conversations/{conversationId}/messages`

Lấy lịch sử tin nhắn theo cursor (phù hợp infinite scroll). **Access: USER (member)**

**Query Parameters:**

| Param    | Type   | Mặc định | Max | Mô tả                                            |
| :------- | :----- | :------- | :-- | :----------------------------------------------- |
| `before` | string | —        | —   | Message ObjectId cursor (lấy các message cũ hơn) |
| `size`   | int    | 30       | 100 | Số message trả về                                |

**Response (200):**

```json
{
  "data": [
    {
      "id": "objectid",
      "conversation_id": "objectid",
      "sender": {
        "id": "uuid",
        "username": "string",
        "avatar_url": "string|null"
      },
      "type": "TEXT",
      "content": "Xin chào",
      "attachments": [],
      "edited_at": null,
      "deleted": false,
      "created_at": "timestamp"
    }
  ],
  "metadata": {
    "next_before": "objectid|null",
    "has_more": true
  }
}
```

**Errors:** `CHAT_CONVERSATION_NOT_FOUND`, `CHAT_MEMBER_REQUIRED`, `FORBIDDEN`

---

### POST `/conversations/{conversationId}/messages`

Gửi tin nhắn vào DM hoặc challenge channel. **Access: USER (member)**

**Request Body:**

```json
{
  "content": "string (optional nếu có attachments, max 2000 chars)",
  "attachments": [
    {
      "media_id": "uuid"
    }
  ]
}
```

**Validation:**

- Bắt buộc có ít nhất 1 trong 2: `content` hoặc `attachments`.
- `attachments.media_id` phải là media đã confirm và user có quyền sử dụng.

**Response (201):**

```json
{
  "data": {
    "id": "objectid",
    "conversation_id": "objectid",
    "sender": {
      "id": "uuid",
      "username": "string",
      "avatar_url": "string|null"
    },
    "type": "TEXT",
    "content": "Mọi người check task ngày 6 nhé",
    "attachments": [],
    "created_at": "timestamp"
  }
}
```

**Errors:** `CHAT_CONVERSATION_NOT_FOUND`, `CHAT_MEMBER_REQUIRED`, `CHAT_EMPTY_MESSAGE`, `CHAT_RATE_LIMITED`, `FORBIDDEN`, `VALIDATION_FAILED`

---

### PATCH `/conversations/{conversationId}/read`

Đánh dấu conversation đã đọc tới message cuối cùng user đã thấy. **Access: USER (member)**

**Request Body:**

```json
{
  "last_message_id": "objectid"
}
```

**Response (200):**

```json
{
  "data": {
    "conversation_id": "objectid",
    "last_read_message_id": "objectid",
    "unread_count": 0,
    "read_at": "timestamp"
  }
}
```

**Errors:** `CHAT_CONVERSATION_NOT_FOUND`, `CHAT_MEMBER_REQUIRED`, `CHAT_MESSAGE_NOT_FOUND`, `FORBIDDEN`

---

### PATCH `/messages/{messageId}`

Sửa nội dung tin nhắn của chính mình. **Access: USER (owner trong 15 phút)**

**Request Body:**

```json
{
  "content": "string (required, 1-2000 chars)"
}
```

**Response (200):**

```json
{
  "data": {
    "id": "objectid",
    "conversation_id": "objectid",
    "content": "Nội dung đã chỉnh sửa",
    "edited_at": "timestamp"
  }
}
```

**Errors:** `CHAT_MESSAGE_NOT_FOUND`, `CHAT_MESSAGE_EDIT_WINDOW_EXPIRED`, `CHAT_EMPTY_MESSAGE`, `FORBIDDEN`

---

### DELETE `/messages/{messageId}`

Xóa tin nhắn. **Access: USER (owner trong 15 phút) / MODERATOR / ADMIN**

**Hành vi:**

- Owner được xóa tin của mình trong vòng 15 phút từ `created_at`.
- MODERATOR/ADMIN có thể xóa bất kỳ message vi phạm.
- Xóa theo cơ chế soft-delete để không vỡ lịch sử hội thoại.

**Response (200):**

```json
{
  "data": null,
  "message": "Xóa tin nhắn thành công"
}
```

**Errors:** `CHAT_MESSAGE_NOT_FOUND`, `CHAT_MESSAGE_EDIT_WINDOW_EXPIRED`, `FORBIDDEN`

---

## 13. Chỉ dẫn cho GitHub Copilot

- **DTOs:** Luôn tạo các lớp `RequestDTO` và `ResponseDTO` riêng biệt.
- **Validation:** Sử dụng `@Valid` và Bean Validation annotations trong Controller.
- **Security Logic:** Kiểm tra quyền sở hữu (Ownership) trước khi cho phép `PUT/PATCH/DELETE`.
- **Logging:** Mọi API thay đổi trạng thái (POST/PUT/PATCH/DELETE) phải gọi Service ghi audit log sang MongoDB.
- **Error Codes:** Sử dụng error codes từ `requirements.md` mục 8 — không tự đặt mới.
- **Pagination:** Mọi endpoint trả về list phải hỗ trợ pagination theo mục 1.2.
