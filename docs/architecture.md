# 1. Tổng quan kiến trúc

Knance backend được xây dựng theo kiểu **Modular Monolith**:

- 1 ứng dụng Spring Boot duy nhất (monolith).
- Bên trong chia thành **nhiều module theo nghiệp vụ**: `auth`, `account`, `transaction`, `saving`, `recurring`, `notification`, …
- Mỗi module lại tuân theo **4 layer**:
  - `api`
  - `application`
  - `domain`
  - `infra`

**Mục tiêu:**

- Tách biệt rõ ràng **business feature** (auth, transaction,…) thay vì chỉ chia controller/service/repository.
- Dễ mở rộng, dễ test, và **có thể tách microservice về sau** nếu cần.

---

# 2. Danh sách module

Các module chính (package ở `com.knance`):

- `auth` – xác thực, đăng ký, đăng nhập, JWT, email verification  
- `user` – profile & dashboard người dùng  
- `account` – ví/tài khoản của user  
- `category` – `MasterCategory` + `UserCategory`  
- `transaction` – giao dịch thu/chi/lưu tiền + analytics cơ bản  
- `recurring` – rule giao dịch định kỳ (template + occurrence)  
- `saving` – `SavingGoal` + `SavingHistory`  
- `notification` – `Notification` + gửi qua Email/WebSocket  
- `outbox` – Outbox pattern lưu `DomainEvent` ra DB  
- `kafka` – cấu hình Kafka + publisher/consumer  
- `common` – shared domain (`Money`, `DomainEvent`, enum chung), exception, util  
- `config` – cấu hình Spring (Security, WebSocket, OpenAPI, Scheduling, …)

**Ví dụ cấu trúc package:**

```text
com.knance
 ├── auth
 ├── account
 ├── category
 ├── transaction
 ├── recurring
 ├── saving
 ├── notification
 ├── outbox
 ├── kafka
 ├── common
 └── config
```

---

# 3. 4 layer bên trong mỗi module

Mỗi module business (`auth`, `account`, `category`, `transaction`, `recurring`, `saving`, `notification`) đều có cùng cấu trúc:

```text
com.knance.<module>.api
com.knance.<module>.application
com.knance.<module>.domain
com.knance.<module>.infra
```

**Ví dụ với module `auth`:**

```text
com.knance.auth
 ├── api
 │   ├── AuthController
 │   └── dto (RegisterRequest, LoginRequest, TokenResponse, …)
 ├── application
 │   └── AuthService (register / login / refresh)
 ├── domain
 │   ├── User, Role, Tokens
 │   ├── EmailVerificationToken
 │   ├── UserFactory, TokenFactory
 │   └── (các value object / nghiệp vụ khác)
 └── infra
     ├── UserEntity (nếu tách riêng)
     ├── UserRepository
     ├── EmailVerificationTokenEntity, EmailVerificationTokenRepository
     └── JwtTokenProvider (tạo JWT thật)
```

## 3.1. Ý nghĩa từng layer

### `api/`

- Chứa: `@RestController`, DTO request/response, mapping **HTTP → use case**.
- Không chứa logic business / logic repository.

### `application/`

- Chứa: `@Service` (use case).
- Điều phối luồng:
  - Nhận DTO từ `api`.
  - Gọi `domain` để xử lý nghiệp vụ.
  - Gọi `infra` để lưu DB / gửi Kafka / gửi email.
- Đây là nơi thường đặt `@Transactional`.

### `domain/`

- Chứa: entity nghiệp vụ, enum, value object, domain event, factory, strategy, …
- Ưu tiên **thuần Java, không phụ thuộc Spring** (không hoặc rất ít annotation).
- Là “trái tim” logic business: tiền, recurring rule, saving goal, validation nghiệp vụ, …

### `infra/` (infrastructure)

- Chứa: mọi thứ dính framework/hạ tầng:
  - JPA (`@Entity`, `JpaRepository`)
  - Kafka producer/consumer
  - Mail adapter, Cloudinary adapter
  - WebSocket (`SimpMessagingTemplate`), …
- Được `application` layer gọi để tương tác với thế giới bên ngoài.

---

# 4. Sơ đồ dependency giữa các layer

## 4.1. Quy tắc phụ thuộc

Luồng phụ thuộc **chỉ đi theo một chiều từ ngoài vào trong**:

```text
api  →  application  →  domain
          ↓
        infra
```

- `domain` **không được** phụ thuộc vào `api`, `application` hay `infra`.
- `infra` **có thể phụ thuộc** `domain`  
  (ví dụ: map giữa Entity và Domain, publish `DomainEvent`).
- `application` là trung tâm:
  - Gọi `domain` (áp dụng business rule, factory, strategy, domain event).
  - Gọi `infra` (repository, kafka, mail, …).

## 4.2. Sơ đồ text

```text
               ┌───────────────────────┐
               │       API layer       │
               │ (REST controllers,    │
               │  request/response DTO)│
               └───────────▲───────────┘
                           │
                           │ calls
                           │
               ┌───────────┴───────────┐
               │   Application layer   │
               │  (Use case services,  │
               │   @Transactional)     │
               └───────▲────────▲──────┘
                       │        │
     uses domain rules │        │ uses infra adapter
                       │        │
        ┌──────────────┘        └───────────────┐
        │                                       │
┌───────┴───────────┐                   ┌───────┴───────────┐
│    Domain layer   │                   │    Infra layer    │
│ (entities, value  │                   │ (JPA, Kafka, Mail,│
│ objects, factories│                   │  WebSocket, ...)  │
└───────────────────┘                   └───────────────────┘
```

