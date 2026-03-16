<!-- instructions/project-structure.md -->

# ChallengeHub: Project Structure

Dự án được tổ chức theo mô hình Monorepo đơn giản hoặc tách biệt tùy chọn. Dưới đây là cấu trúc khuyến nghị để tối ưu hóa khả năng đọc hiểu của GitHub Copilot.

---

## 1. Backend (Java Spring Boot)

Cấu trúc theo tầng (Layered Architecture) kết hợp với Domain-driven design nhẹ.

```text
challenge-hub-backend/
├── src/main/java/com/challengehub/
│   ├── config/             # Security, Mongo/Postgres Config, Cloud Storage
│   ├── controller/         # REST Endpoints (v1)
│   ├── dto/                # Request/Response Data Transfer Objects
│   │   ├── request/
│   │   └── response/
│   ├── entity/
│   │   ├── postgres/       # JPA Entities (User, Challenge, Submission)
│   │   └── mongodb/        # Document Entities (AuditLog, Interaction, Chat)
│   ├── exception/          # Global Exception Handler
│   ├── repository/
│   │   ├── postgres/       # Spring Data JPA Interfaces
│   │   └── mongodb/        # Spring Data MongoDB Interfaces
│   ├── service/            # Business Logic (Interface & Impl)
│   └── security/           # JWT Filter, UserDetailsImpl
├── src/main/resources/
│   ├── application.yml     # Cấu hình chung
│   └── application-dev.yml # Cấu hình cho máy local
└── pom.xml
```

---

## 2. Frontend (React TypeScript + Vite)

Sử dụng Feature-based folder structure để dễ dàng bảo trì.

```text
challenge-hub-frontend/
├── src/
│   ├── api/                # Axios instance, API hooks (TanStack Query)
│   ├── assets/             # Images, Icons, Global Styles
│   ├── components/         # Common UI (Button, Input, Layout)
│   ├── features/           # Modules chính của ứng dụng
│   │   ├── auth/           # Login, Register
│   │   ├── challenges/     # List, Detail, Create
│   │   ├── submissions/    # Submit form, Feedback
│   │   ├── social/         # Comments, Reactions, Leaderboard
│   │   └── messaging/      # Direct Message, Challenge Group Chat, Channels
│   ├── hooks/              # Custom hooks dùng chung
│   ├── store/              # State management (Zustand/Redux)
│   └── types/              # TypeScript Interfaces (khớp với DTO Backend)
├── public/
├── tailwind.config.js
└── vite.config.ts
```

---

## 3. DevOps & Deployment (Gốc dự án)

Phục vụ việc deploy lên VPS.

```text
/
├── .github/
│   └── copilot-instructions.md
├── instructions/           # Tài liệu thiết kế đã tạo
├── docker-compose.yml        # Chạy Postgres, Mongo, Redis, App (gốc dự án)
├── docker/
│   └── nginx.conf            # Cấu hình Reverse Proxy cho VPS
└── .gitignore
```

---

## 4. Chỉ dẫn cho GitHub Copilot khi làm việc với cấu trúc này

- **Khi tạo Entity mới:** Nếu là dữ liệu giao dịch quan trọng, hãy tạo trong `entity/postgres`. Nếu là log, social interaction hoặc chat realtime, hãy tạo trong `entity/mongodb`.
- **Khi tạo API:** Luôn kiểm tra xem `dto/request` đã có đủ Validation `@NotBlank`, `@Size` chưa.
- **Frontend Sync:** Các Interface trong `src/types/` phải luôn khớp với `ResponseDTO` từ Backend.
- **Tích hợp Mobile:** Hãy viết các Service ở Frontend sao cho logic xử lý dữ liệu (data mapping) nằm riêng, để sau này có thể copy sang React Native một cách nhanh nhất.
