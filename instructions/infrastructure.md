<!-- instructions/infrastructure.md -->

# ChallengeHub: Đặc tả Hạ tầng & Triển khai (Infrastructure Specification)

Tài liệu này định nghĩa cấu hình môi trường chạy (Runtime), các dịch vụ Docker, CI/CD pipeline và chiến lược triển khai hệ thống ChallengeHub.

---

## 1. Stack hạ tầng (Infrastructure Stack)

Dự án sử dụng mô hình Container hóa để đảm bảo tính nhất quán từ môi trường Local đến VPS.

| Thành phần           | Công nghệ                     | Mục đích                                               |
| :------------------- | :---------------------------- | :----------------------------------------------------- |
| **Container Engine** | Docker & Docker Compose       | Đóng gói và quản lý các dịch vụ.                       |
| **Relational DB**    | PostgreSQL 15                 | Lưu trữ dữ liệu giao dịch (ACID).                      |
| **Document DB**      | MongoDB 6.0                   | Lưu trữ logs, social feed, comments, chat messages.    |
| **In-memory DB**     | Redis 7                       | Caching, Ranking (ZSET), Leaderboard, Token Blacklist. |
| **Media Storage**    | MinIO (Local) / Cloudflare R2 | Lưu trữ ảnh, video bài nộp.                            |
| **Reverse Proxy**    | Nginx                         | Điều hướng traffic và quản lý SSL.                     |
| **Email (MVP)**      | SMTP (Mailgun / Resend)       | Gửi email xác nhận, thông báo.                         |

---

## 2. Docker Compose Configuration

File `docker-compose.yml` tại thư mục gốc của dự án:

```yaml
version: "3.8"

services:
  # 1. Spring Boot Backend
  backend:
    build:
      context: ./challenge-hub-backend
      dockerfile: Dockerfile
    container_name: ch-backend
    restart: always
    env_file: .env
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - mongodb
      - redis
      - minio
    networks:
      - ch-network

  # 2. React Frontend (Build & Serve via Nginx)
  frontend:
    build:
      context: ./challenge-hub-frontend
      dockerfile: Dockerfile
    container_name: ch-frontend
    restart: always
    networks:
      - ch-network

  # 3. PostgreSQL
  postgres:
    image: postgres:15-alpine
    container_name: ch-postgres
    restart: always
    environment:
      POSTGRES_DB: challengehub
      POSTGRES_USER: ${DB_USER:-admin}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-password123}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    networks:
      - ch-network

  # 4. MongoDB
  mongodb:
    image: mongo:6.0
    container_name: ch-mongodb
    restart: always
    ports:
      - "27017:27017"
    volumes:
      - mongodata:/data/db
    networks:
      - ch-network

  # 5. Redis
  redis:
    image: redis:7-alpine
    container_name: ch-redis
    restart: always
    ports:
      - "6379:6379"
    networks:
      - ch-network

  # 6. MinIO (Local Storage)
  minio:
    image: minio/minio
    container_name: ch-minio
    restart: always
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY:-minioadmin}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY:-minioadmin}
    command: server /data --console-address ":9001"
    volumes:
      - miniodata:/data
    networks:
      - ch-network

  # 7. Nginx Reverse Proxy
  nginx:
    image: nginx:alpine
    container_name: ch-nginx
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./docker/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./docker/ssl:/etc/nginx/ssl:ro
    depends_on:
      - backend
      - frontend
    networks:
      - ch-network

volumes:
  pgdata:
  mongodata:
  miniodata:

networks:
  ch-network:
    driver: bridge
```

---

## 3. Quản lý Biến môi trường (.env)

```bash
# === Application ===
APP_NAME=ChallengeHub
APP_ENV=development
APP_PORT=8080
APP_TIMEZONE=UTC

# === Database (PostgreSQL) ===
DB_USER=admin
DB_PASSWORD=password123
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/challengehub
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}

# === MongoDB ===
SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/challengehub

# === Redis ===
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379

# === JWT ===
JWT_SECRET=your-256-bit-secret-key-change-in-production
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# === Media Provider (minio | cloudinary | r2) ===
MEDIA_PROVIDER=minio

# === S3 Compatible Config (MinIO / R2) ===
STORAGE_S3_ENDPOINT=http://minio:9000
STORAGE_S3_ACCESS_KEY=minioadmin
STORAGE_S3_SECRET_KEY=minioadmin
STORAGE_S3_BUCKET=challenge-assets

# === Cloudinary Config (khi MEDIA_PROVIDER=cloudinary) ===
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=

# === Email (SMTP) ===
MAIL_ENABLED=false
MAIL_HOST=smtp.mailgun.org
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=noreply@challengehub.app

# === CORS ===
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://challengehub.app

# === Rate Limiting ===
RATE_LIMIT_PUBLIC=100
RATE_LIMIT_AUTH=300
```

---

## 4. Chiến lược Triển khai trên VPS

### 4.1. Network & Security

- **Internal Network:** Các container (DB, Redis) giao tiếp qua Docker bridge network `ch-network`.
- **Expose:** Chỉ mở port 80 (HTTP) và 443 (HTTPS) ra internet thông qua Nginx.
- **Firewall:** Đóng port 5432, 27017, 6379, 9000 trên IP Public của VPS.
- **SSL:** Let's Encrypt (certbot) tự động renew.

### 4.2. Reverse Proxy (Nginx)

Cấu hình Nginx điều hướng theo path:

- `challengehub.app/*` → Frontend container (React static files).
- `challengehub.app/api/*` → Proxy pass tới backend:8080 (Spring Boot).
- `challengehub.app/ws/*` → WebSocket upgrade tới backend:8080.

### 4.3. Database Backups

- **PostgreSQL:** `pg_dump` chạy hàng ngày lúc 02:00 UTC, giữ 7 bản gần nhất.
- **MongoDB:** `mongodump` chạy hàng ngày lúc 02:30 UTC, giữ 7 bản.
- Backup lưu tại `/backups/` trên VPS và sync lên S3/R2 hàng tuần.

### 4.4. Realtime Messaging (WebSocket) - Vận hành

- Tất cả traffic chat realtime đi qua endpoint `/ws` (STOMP over WebSocket).
- Nếu scale backend > 1 instance, cần cấu hình **sticky session** ở Nginx hoặc message broker relay để tránh mất session realtime.
- Mọi kết nối WebSocket phải xác thực Access Token tương tự REST và kiểm tra blacklist Redis.

---

## 5. CI/CD Pipeline (GitHub Actions)

### 5.1. Workflow: Test & Build (On PR / Push to main)

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  # === Backend Tests ===
  backend-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: challengehub_test
          POSTGRES_USER: admin
          POSTGRES_PASSWORD: test123
        ports: ["5432:5432"]
      mongodb:
        image: mongo:6.0
        ports: ["27017:27017"]
      redis:
        image: redis:7-alpine
        ports: ["6379:6379"]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
          cache: maven
      - name: Run Backend Tests
        working-directory: challenge-hub-backend
        run: mvn verify -Dspring.profiles.active=test

  # === Frontend Tests ===
  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: challenge-hub-frontend/package-lock.json
      - name: Install & Test
        working-directory: challenge-hub-frontend
        run: |
          npm ci
          npm run lint
          npm run test -- --run

  # === Build Docker Images ===
  build:
    needs: [backend-test, frontend-test]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build Backend Image
        run: docker build -t challengehub-backend:latest ./challenge-hub-backend
      - name: Build Frontend Image
        run: docker build -t challengehub-frontend:latest ./challenge-hub-frontend
```

### 5.2. Workflow: Deploy to VPS (On push to main — manual trigger)

```yaml
# .github/workflows/deploy.yml
name: Deploy to VPS

on:
  workflow_dispatch:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            cd /opt/challenge-hub
            git pull origin main
            docker compose pull
            docker compose up -d --build
            docker system prune -f
```

### 5.3. Required GitHub Secrets

| Secret        | Mô tả                        |
| :------------ | :--------------------------- |
| `VPS_HOST`    | IP/Domain của VPS            |
| `VPS_USER`    | SSH username                 |
| `VPS_SSH_KEY` | SSH private key              |
| `DB_PASSWORD` | Postgres password production |
| `JWT_SECRET`  | JWT signing key production   |

---

## 6. Email Service Specification

### 6.1. Overview

Hệ thống gửi email cho các sự kiện quan trọng. Tại MVP, email là **optional** (có thể tắt qua `MAIL_ENABLED=false`).

### 6.2. Email Provider Strategy

| Môi trường  | Provider     | Mục đích                       |
| :---------- | :----------- | :----------------------------- |
| Development | Console Log  | In ra console thay vì gửi thật |
| MVP         | Mailgun Free | 5,000 emails/tháng miễn phí    |
| Production  | Resend / SES | Scale lớn, deliverability cao  |

### 6.3. Email Templates

| Template              | Trigger                   | Mô tả                                      |
| :-------------------- | :------------------------ | :----------------------------------------- |
| `welcome`             | Sau khi register          | Chào mừng user mới + link verify (Phase 2) |
| `submission_approved` | Submission → APPROVED     | Thông báo bài nộp đã duyệt + điểm          |
| `submission_rejected` | Submission → REJECTED     | Thông báo bài nộp bị từ chối + lý do       |
| `streak_warning`      | Cron: 20:00 UTC hàng ngày | Nhắc submit nếu chưa submit hôm nay        |
| `password_reset`      | Forgot password (Phase 2) | Link reset mật khẩu                        |

### 6.4. Email Service Interface

```java
public interface EmailService {
    void sendWelcome(String toEmail, String username);
    void sendSubmissionApproved(String toEmail, String challengeTitle, int score);
    void sendSubmissionRejected(String toEmail, String challengeTitle, String reason);
    void sendStreakWarning(String toEmail, String username, int currentStreak);
}
```

- Sử dụng `@Async` để không chặn luồng chính.
- Bean implementation dựa trên `MAIL_ENABLED` và environment.

---

## 7. Scheduled Jobs

Danh sách các Scheduled Job trong hệ thống:

| Job Name                | Schedule            | Mô tả                                                                |
| :---------------------- | :------------------ | :------------------------------------------------------------------- |
| `ChallengeStatusJob`    | Mỗi 1 phút          | Auto-transition PUBLISHED→ONGOING, ONGOING→ENDED                     |
| `StreakResetJob`        | 00:15 UTC hàng ngày | Reset streak_count = 0 cho users đứt chuỗi                           |
| `StreakWarningEmailJob` | 20:00 UTC hàng ngày | Gửi email nhắc submit cho users có streak > 0 mà chưa submit hôm nay |

---

## 8. Giám sát & Logs

- **Application Logs:** Ghi ra file + stdout (để Docker logs capture).
- **Audit Logs:** Ghi vào MongoDB collection `audit_logs` (xem db-schema.md).
- **Health Check:** Spring Boot Actuator endpoint `/actuator/health`.
- **Metrics (Phase 2):** Prometheus + Grafana stack.

### 8.1. Log Levels

| Environment | Level   |
| :---------- | :------ |
| Development | DEBUG   |
| Production  | INFO    |
| Audit Logs  | Luôn ON |
