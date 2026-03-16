<!-- instructions/media-storage.md -->

# ChallengeHub: Media Storage Specification

Tài liệu này định nghĩa cách hệ thống quản lý tệp tin đa phương tiện, đảm bảo tính độc lập giữa logic nghiệp vụ và hạ tầng lưu trữ (Infrastructure Decoupling).

---

## 1. Media Overview

Hệ thống hỗ trợ các loại tệp sau:

- **Loại tệp:** Images, Videos.
- **Định dạng cho phép:** `.jpg`, `.png`, `.webp`, `.mp4`.
- **Ràng buộc kích thước:**
  - Max Image Size: **10MB**
  - Max Video Size: **200MB**

---

## 2. Media Storage Abstraction

Lớp `MediaStorageService` là interface duy nhất mà Domain Logic được phép tương tác.

### Interface Methods:

- `generateUploadUrl(fileName, contentType)`: Tạo URL có chữ ký để Client upload trực tiếp.
- `getPublicUrl(fileKey)`: Lấy URL công khai để hiển thị.
- `delete(fileKey)`: Xóa tệp khỏi storage provider.

---

## 3. Storage Providers & Environment

Hệ thống chuyển đổi Provider thông qua cấu hình biến môi trường `MEDIA_PROVIDER`.

| Provider          | Môi trường            | Mục đích                                                     |
| :---------------- | :-------------------- | :----------------------------------------------------------- |
| **MinIO**         | Development / Testing | Tương thích S3 API, chạy local qua Docker.                   |
| **Cloudinary**    | MVP Deployment        | Tối ưu ảnh/video tự động, hỗ trợ CDN sẵn có.                 |
| **Cloudflare R2** | Production Scale      | Chi phí băng thông 0đ, lưu trữ S3-compatible dung lượng lớn. |

---

## 4. Quy trình Upload (Client-Direct Upload)

Để tối ưu hiệu năng cho Server VPS, quy trình upload được thực hiện như sau:

1. **Frontend:** User chọn file → Gửi metadata (`file_name`, `content_type`, `file_size`) tới `POST /api/v1/media/upload-url`.
2. **Backend:** Validate (size, type) → gọi `MediaStorageService.generateUploadUrl()` → tạo record `media` (Postgres, status=PENDING) → trả về `{ media_id, upload_url, file_key }`.
3. **Frontend:** Nhận Signed URL → Upload tệp trực tiếp lên **Media Storage Provider** (PUT).
4. **Frontend:** Sau khi upload xong, gửi `POST /api/v1/media/confirm/{mediaId}`.
5. **Backend:** Verify file tồn tại trên storage → set `media.status = CONFIRMED`, `file_url = public URL` → trả về media object.

---

## 5. Cấu hình Môi trường

```properties
# Chọn provider: minio, cloudinary, hoặc r2
MEDIA_PROVIDER=minio

# Cấu hình chi tiết sẽ được nạp dựa trên provider đã chọn
STORAGE_S3_ENDPOINT=...
STORAGE_S3_KEY=...
STORAGE_S3_SECRET=...
```
