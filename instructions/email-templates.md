<!-- instructions/email-templates.md -->

# ChallengeHub: Email Templates Specification

Tài liệu này định nghĩa các template email chuẩn, gồm subject, nội dung, và biến placeholder. Mục tiêu là giúp backend và frontend (nếu cần preview) thống nhất nội dung.

---

## 1. Nguyên tắc chung

- Tất cả email sử dụng ngôn ngữ thân thiện, ngắn gọn.
- Format HTML + text fallback (multipart/alternative).
- Tất cả timestamp hiển thị theo timezone local của user (convert ở backend trước khi render template hoặc ghi rõ “UTC”).
- Mọi link nhạy cảm (reset password, verify email) phải có hạn sử dụng và one-time token.

Placeholder được viết dưới dạng `{{variableName}}`.

---

## 2. Welcome Email (`welcome`)

- **Trigger:** Sau khi đăng ký thành công (nếu `MAIL_ENABLED=true`).
- **Subject:** `Chào mừng {{username}} đến với ChallengeHub!`
- **Placeholders:**
  - `{{username}}`: tên đăng nhập.

**Body (HTML – rút gọn):**

```html
<h1>Chào mừng {{username}} 👋</h1>
<p>
  Cảm ơn bạn đã tham gia ChallengeHub. Bắt đầu bằng cách tham gia một thử thách
  phù hợp với bạn!
</p>
<p>Truy cập <a href="{{appUrl}}">ChallengeHub</a> để khám phá ngay.</p>
```

---

## 3. Submission Approved (`submission_approved`)

- **Trigger:** Khi submission chuyển sang trạng thái APPROVED.
- **Subject:** `Bài nộp của bạn đã được duyệt: {{challengeTitle}}`
- **Placeholders:**
  - `{{username}}`
  - `{{challengeTitle}}`
  - `{{taskTitle}}`
  - `{{score}}`
  - `{{maxScore}}`
  - `{{submittedAt}}`
  - `{{appUrl}}` (link tới chi tiết challenge/submission)

**Body (HTML – rút gọn):**

```html
<h1>Bài nộp đã được duyệt 🎉</h1>
<p>Xin chúc mừng {{username}},</p>
<p>
  Bài nộp cho <strong>{{taskTitle}}</strong> trong thử thách
  <strong>{{challengeTitle}}</strong> đã được duyệt với số điểm
  <strong>{{score}} / {{maxScore}}</strong>.
</p>
<p>Bạn có thể xem chi tiết tại <a href="{{appUrl}}">đây</a>.</p>
```

---

## 4. Submission Rejected (`submission_rejected`)

- **Trigger:** Khi submission chuyển sang trạng thái REJECTED.
- **Subject:** `Bài nộp của bạn chưa đạt: {{challengeTitle}}`
- **Placeholders:**
  - `{{username}}`
  - `{{challengeTitle}}`
  - `{{taskTitle}}`
  - `{{rejectReason}}`
  - `{{appUrl}}`

**Body (HTML – rút gọn):**

```html
<h1>Bài nộp của bạn chưa đạt</h1>
<p>Chào {{username}},</p>
<p>
  Bài nộp cho <strong>{{taskTitle}}</strong> trong thử thách
  <strong>{{challengeTitle}}</strong> hiện chưa đạt yêu cầu.
</p>
<p><strong>Lý do:</strong> {{rejectReason}}</p>
<p>Bạn có thể chỉnh sửa và nộp lại tại <a href="{{appUrl}}">đây</a>.</p>
```

---

## 5. Streak Warning (`streak_warning`)

- **Trigger:** `StreakWarningEmailJob` lúc 20:00 UTC cho user có streak > 0 nhưng chưa có submission APPROVED trong ngày.
- **Subject:** `Giữ vững streak {{currentStreak}} ngày của bạn trên ChallengeHub!`
- **Placeholders:**
  - `{{username}}`
  - `{{currentStreak}}`
  - `{{appUrl}}`

**Body (HTML – rút gọn):**

```html
<h1>Đừng để streak của bạn dừng lại!</h1>
<p>Chào {{username}},</p>
<p>
  Bạn đang có streak {{currentStreak}} ngày liên tiếp. Hãy hoàn thành ít nhất
  một thử thách hôm nay để duy trì chuỗi này.
</p>
<p>Truy cập <a href="{{appUrl}}">ChallengeHub</a> để tiếp tục.</p>
```

---

## 6. Password Reset (`password_reset`) – Phase 2

- **Trigger:** Khi user yêu cầu quên mật khẩu (chưa implement trong MVP nhưng cần thiết kế trước).
- **Subject:** `Đặt lại mật khẩu ChallengeHub của bạn`
- **Placeholders:**
  - `{{username}}`
  - `{{resetLink}}` (chứa token 1 lần, có expiry)
  - `{{expiresInMinutes}}`

**Body (HTML – rút gọn):**

```html
<h1>Đặt lại mật khẩu</h1>
<p>Chào {{username}},</p>
<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
<p>
  Nhấn vào link bên dưới để đặt lại mật khẩu (link sẽ hết hạn sau
  {{expiresInMinutes}} phút):
</p>
<p><a href="{{resetLink}}">Đặt lại mật khẩu</a></p>
<p>Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.</p>
```

---

## 7. Unsubscribe & Footer chuẩn

Tất cả email marketing (nếu có phase sau) phải có footer:

```text
Bạn nhận được email này vì bạn đã đăng ký ChallengeHub.
Nếu không muốn nhận các email tương tự, hãy cập nhật cài đặt thông báo trong ứng dụng.
```

Đối với email giao dịch (transactional) như reset password, submission approved/rejected, welcome email: **không bắt buộc** unsubscribe link nhưng vẫn phải tuân thủ quy định pháp lý tại môi trường triển khai.
