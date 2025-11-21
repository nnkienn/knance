# Knance – Domain Design (Chi tiết)

Tài liệu này mô tả **các entity, enum, quan hệ & use case chính** trong hệ thống Knance (personal finance).

---

## I. USER & AUTH

### 1. Entity: User

**Ý nghĩa:** Đại diện tài khoản người dùng trong hệ thống, gồm phần **authentication**, **profile** và **meta**.

#### Field chi tiết

| Field                 | Kiểu                | Bắt buộc | Mô tả |
|----------------------|---------------------|----------|------|
| `id`                 | Long                | ✔        | Khóa chính |
| `email`              | String              | ✔        | Email đăng nhập, duy nhất trong hệ thống |
| `passwordHash`       | String              | ✔        | Mật khẩu đã mã hóa (BCrypt, Argon2, …) |
| `roles`              | Set\<Role>         | ✔        | Danh sách quyền (USER, ADMIN) |
| `emailVerifiedAt`    | Instant?            | ✖        | Thời gian email được xác thực (null nếu chưa) |
| `enabled`            | boolean             | ✔        | Tài khoản có được phép đăng nhập không |

**Profile**

| Field                 | Kiểu                | Bắt buộc | Mô tả |
|----------------------|---------------------|----------|------|
| `fullName`           | String              | ✔        | Họ tên hiển thị |
| `avatarUrl`          | String?             | ✖        | Link ảnh đại diện |
| `dateOfBirth`        | LocalDate?          | ✖        | Ngày sinh |
| `gender`             | Gender (enum)       | ✖        | MALE, FEMALE, OTHER |
| `phoneNumber`        | String?             | ✖        | Số điện thoại |
| `preferredLanguage`  | String              | ✔        | Ngôn ngữ ưu tiên (`"vi"`, `"en"`, …) |
| `timezone`           | String              | ✔        | Múi giờ (`"Asia/Ho_Chi_Minh"`, …) |
| `notificationChannel`| NotificationChannel | ✔        | Kênh nhận thông báo: PHONE, EMAIL, BOTH, NONE |

**Social Login**

| Field        | Kiểu    | Bắt buộc | Mô tả |
|--------------|---------|----------|------|
| `googleId`   | String? | ✖        | ID Google, nếu user đăng nhập qua Google |
| `githubId`   | String? | ✖        | ID GitHub |

**Meta**

| Field       | Kiểu    | Bắt buộc | Mô tả |
|-------------|---------|----------|------|
| `createdAt` | Instant | ✔        | Ngày tạo tài khoản |
| `updatedAt` | Instant | ✔        | Ngày cập nhật cuối |

#### Enum: Role

- `USER`
- `ADMIN`

(Option: lưu thêm bảng `roles` nếu cần quản lý động.)

---

### 2. Entity: EmailVerificationToken

**Ý nghĩa:** Lưu token dùng để xác thực email cho User.

| Field       | Kiểu    | Bắt buộc | Mô tả |
|-------------|---------|----------|------|
| `id`        | Long    | ✔        | Khóa chính |
| `token`     | String  | ✔        | Chuỗi token ngẫu nhiên/UUID |
| `user`      | User    | ✔        | User sở hữu token |
| `expiresAt` | Instant | ✔        | Thời điểm hết hạn |
| `used`      | boolean | ✔        | Đã được dùng hay chưa |

**Rule:**

- Token chỉ dùng 1 lần: `used == true` sau khi xác thực.
- Không chấp nhận token đã hết hạn (`now > expiresAt`).

---

### 3. Use Case liên quan đến User/Auth

- `register`  
  - Tạo User mới, set role = USER, gửi email verification (optional).
- `login`  
  - Kiểm tra email/password, tạo Access + Refresh token.
- `refresh`  
  - Cấp mới access token từ refresh token.
- `updateUserProfile`  
  - Cập nhật thông tin profile (tên, avatar, ngôn ngữ, timezone,…).
- `changePassword`  
  - Yêu cầu mật khẩu cũ, cập nhật mật khẩu mới.
- `changeAvatar`  
  - Upload avatar mới, cập nhật `avatarUrl`.
- `viewUserDashboard`  
  - Tổng hợp dữ liệu: số account, số transaction gần đây, tổng thu/chi/tiết kiệm, …


---

## II. ACCOUNT

**Ý nghĩa:** Mỗi user có thể có nhiều tài khoản tiền: ví chính, ví tiết kiệm, thẻ ngân hàng…

### 1. Entity: Account

| Field       | Kiểu             | Bắt buộc | Mô tả |
|-------------|------------------|----------|------|
| `id`        | Long             | ✔        | Khóa chính |
| `user`      | User             | ✔        | Chủ sở hữu account |
| `name`      | String           | ✔        | Tên ví (VD: “Ví chính”, “Tiết kiệm laptop”) |
| `balance`   | Money/BigDecimal | ✔        | Số dư hiện tại |
| `currency`  | String           | ✔        | Mã tiền tệ (VND, USD, …) |
| `type`      | AccountType      | ✔        | MAIN, SAVING, BANK, CREDIT_CARD,… |
| `isDefault` | boolean          | ✔        | Có phải ví mặc định không |
| `createdAt` | Instant          | ✔        | Ngày tạo |
| `updatedAt` | Instant          | ✔        | Ngày cập nhật |

### 2. Use Case

- `createAccount`
- `getUserAccounts`
- `updateAccount`
- `deleteAccount` (hoặc soft delete)
- `setDefaultAccount`

**Rule:**

- Một user có **ít nhất 1 account** (ví chính).
- Chỉ 1 account được đánh dấu `isDefault = true` / user.

---

## III. CATEGORY

Tách làm 2 lớp:

- Category “mẫu” do hệ thống định nghĩa (`MasterCategory`).
- Category theo user (`UserCategory`).

---

### 1. Enum: CategoryType

- `EXPENSE`
- `INCOME`
- `SAVING`

---

### 2. Entity: MasterCategory

**Ý nghĩa:** Category chuẩn (ăn uống, lương, đi lại…) để user có thể copy dùng.

| Field           | Kiểu         | Bắt buộc | Mô tả |
|-----------------|--------------|----------|------|
| `id`            | Long         | ✔        | Khóa chính |
| `name`          | String       | ✔        | Tên category |
| `type`          | CategoryType | ✔        | EXPENSE / INCOME / SAVING |
| `icon`          | String       | ✔        | Icon/tên biểu tượng |
| `isDefault`     | boolean      | ✔        | Hệ thống tạo sẵn hay do admin thêm |

**Use case:**

- `getAllMasterCategories`
- `getMasterCategoryById`
- `filterMasterCategoriesByType`

---

### 3. Entity: UserCategory

**Ý nghĩa:** Category cá nhân của user (có thể copy từ master hoặc tự tạo).

| Field           | Kiểu           | Bắt buộc | Mô tả |
|-----------------|----------------|----------|------|
| `id`            | Long           | ✔        | Khóa chính |
| `name`          | String         | ✔        | Tên category |
| `icon`          | String         | ✔        | Icon hiển thị |
| `user`          | User           | ✔        | Chủ sở hữu |
| `masterCategory`| MasterCategory?| ✖        | Category mẫu gốc (nếu có) |
| `type`          | CategoryType   | ✔        | EXPENSE / INCOME / SAVING |
| `createdAt`     | Instant        | ✔        | Ngày tạo |
| `updatedAt`     | Instant        | ✔        | Ngày cập nhật |

**Use case chính:**

- CRUD:
  - `createUserCategory`
  - `getUserCategories`
  - `updateUserCategory`
  - `deleteUserCategory`
- Filter/Search:
  - `filterUserCategoriesByType(type)`
  - `filterUserCategoriesByMaster(masterCategoryId)`
  - `searchUserCategoriesByName(keyword)`
- Khởi tạo:
  - `copyDefaultCategoriesForUser(userId)`
- Analytics (kết hợp với Transaction):
  - `getTotalAmountByCategory`
  - `getExpensesByCategory`
  - `getTopExpenseCategories`
  - `countCategoriesByType`

---

## IV. TRANSACTION & RECURRENCE

### 1. Enum: TransactionType

- `INCOME`
- `EXPENSE`
- `SAVING`

### 2. Enum: PaymentMethod

- `CASH`
- `BANK`
- `MOMO`
- `CREDIT_CARD`
- …

### 3. Entity: Transaction

**Ý nghĩa:** Mọi khoản thu/chi/lưu tiền của user.

| Field                 | Kiểu             | Bắt buộc | Mô tả |
|-----------------------|------------------|----------|------|
| `id`                  | Long             | ✔        | Khóa chính |
| `user`                | User             | ✔        | Chủ sở hữu (để query nhanh) |
| `account`             | Account          | ✔        | Ví được dùng |
| `userCategory`        | UserCategory     | ✔        | Category của giao dịch |
| `savingGoal`          | SavingGoal?      | ✖        | Mục tiêu tiết kiệm liên quan (nếu có) |
| `amount`              | Money/BigDecimal | ✔        | Số tiền |
| `type`                | TransactionType  | ✔        | INCOME/EXPENSE/SAVING |
| `paymentMethod`       | PaymentMethod    | ✔        | Cash, bank, momo… |
| `note`                | String?          | ✖        | Ghi chú |
| `transactionDate`     | LocalDateTime    | ✔        | Thời điểm giao dịch xảy ra |
| `recurringOccurrence` | RecurringOccurrence? | ✖    | Nếu được sinh ra từ recurring |
| `createdAt`           | Instant          | ✔        | Ngày tạo record |
| `updatedAt`           | Instant          | ✔        | Ngày cập nhật |

**Rule:**

- `amount > 0`.  
- `type` phải khớp với `UserCategory.type`.  
- `account.user == user` (bảo vệ dữ liệu).

---

### 4. Use case: Transaction

CRUD + Query:

- `createTransaction`
- `getTransactionById`
- `getUserTransactions(userId)`
- `updateTransaction`
- `deleteTransaction` (có thể là soft delete)

Filter:

- `getTransactionsFiltered(filterSpec)`
- `getTransactionsFilteredPaged(filterSpec, page, size)`
- `getLatestTransactions(limit)`

Import/Export:

- `exportTransactionsToCsv`
- `exportTransactionsToPdf`
- `importTransactionsFromCsv`

Analytics:

- `getMonthlySummary(userId, month/year)`
- `getCategoryBreakdown(userId, period)`
- `getPaymentMethodBreakdown(userId, period)`
- `getMonthlyCards` (số liệu tổng quan cho dashboard)
- `getTotalSaving(userId, period)`
- `getTotalIncome(userId, period)`
- `getTotalExpense(userId, period)`

Recurring:

- `postFromRecurring(recurringOccurrenceId)`
  - Tạo Transaction từ một kỳ định kỳ, mark RecurringOccurrence = POSTED.

---

## V. RECURRING (GIAO DỊCH ĐỊNH KỲ)

### 1. Enum: RecurringFrequency

- `DAILY`
- `WEEKLY`
- `MONTHLY`
- `YEARLY`

### 2. Enum: RecurringStatus

- `PLANNED` – kỳ dự kiến  
- `POSTED` – đã tạo transaction thật  
- `REVIEW` – cần xác nhận tay (ví dụ: vượt hạn mức)  
- `SKIPPED` – bỏ qua kỳ này  
- `MISSED` – quá hạn chưa xử lý

---

### 3. Entity: RecurringTemplate

**Ý nghĩa:** Định nghĩa rule thu/chi định kỳ mà user muốn.

| Field             | Kiểu             | Bắt buộc | Mô tả |
|-------------------|------------------|----------|------|
| `id`              | Long             | ✔        | Khóa chính |
| `user`            | User             | ✔        | Chủ sở hữu rule |
| `account`         | Account          | ✔        | Ví dùng cho các kỳ định kỳ |
| `userCategory`    | UserCategory     | ✔        | Category mặc định |
| `amount`          | Money/BigDecimal | ✔        | Số tiền mỗi kỳ (có thể cho phép thay đổi) |
| `type`            | TransactionType  | ✔        | INCOME/EXPENSE/SAVING |
| `paymentMethod`   | PaymentMethod    | ✔        | Phương thức thanh toán |
| `frequency`       | RecurringFrequency | ✔      | DAILY/WEEKLY/MONTHLY/YEARLY |
| `startAt`         | LocalDate        | ✔        | Ngày bắt đầu |
| `endAt`           | LocalDate?       | ✖        | Ngày kết thúc (nếu có) |
| `active`          | boolean          | ✔        | Còn hoạt động hay không |
| `createdAt`       | Instant          | ✔        | Ngày tạo |
| `updatedAt`       | Instant          | ✔        | Ngày cập nhật |

**Rule:**

- `startAt >= today`.  
- Nếu `endAt != null` thì `endAt >= startAt`.

---

### 4. Entity: RecurringOccurrence

**Ý nghĩa:** Đại diện cho **một kỳ cụ thể** của RecurringTemplate.

| Field             | Kiểu                | Bắt buộc | Mô tả |
|-------------------|---------------------|----------|------|
| `id`              | Long                | ✔        | Khóa chính |
| `template`        | RecurringTemplate   | ✔        | Template gốc |
| `occurrenceAt`    | LocalDate           | ✔        | Ngày kỳ này dự kiến/diễn ra |
| `status`          | RecurringStatus     | ✔        | PLANNED/POSTED/REVIEW/SKIPPED/MISSED |
| `amountExpected`  | Money/BigDecimal    | ✔        | Số tiền dự kiến |
| `postedTransaction` | Transaction?      | ✖        | Transaction thực tế được tạo (nếu POSTED) |
| `createdAt`       | Instant             | ✔        | Ngày tạo |
| `updatedAt`       | Instant             | ✔        | Ngày cập nhật |

**Business flow (tóm tắt):**

- Khi tạo `RecurringTemplate` → sinh **occurrence đầu tiên** (`PLANNED`) tại `startAt`.
- Cron job:
  - Lấy tất cả occurrence:
    - `status = PLANNED`
    - `occurrenceAt <= today`
  - Tùy rule:
    - Tự động tạo Transaction → set `status = POSTED` + sinh occurrence tiếp theo.
    - Hoặc, nếu cần xác nhận tay → set `status = REVIEW`.
- User có thể:
  - Confirm occurrence → tạo Transaction (POSTED) + tạo kỳ mới.
  - Skip → set `status = SKIPPED`.
  - Nếu quá hạn không xử lý → `MISSED`.

---

## VI. SAVING GOAL (MỤC TIÊU TIẾT KIỆM)

### 1. Enum: SavingGoalStatus

- `IN_PROGRESS`  – đang tiết kiệm  
- `ACHIEVED`     – đã đạt mục tiêu  
- `CANCELLED`    – hủy bỏ

---

### 2. Entity: SavingGoal

| Field           | Kiểu             | Bắt buộc | Mô tả |
|-----------------|------------------|----------|------|
| `id`            | Long             | ✔        | Khóa chính |
| `user`          | User             | ✔        | Chủ sở hữu |
| `name`          | String           | ✔        | Tên mục tiêu (VD: “Mua laptop”) |
| `targetAmount`  | Money/BigDecimal | ✔        | Số tiền mục tiêu |
| `currentAmount` | Money/BigDecimal | ✔        | Số đã tiết kiệm |
| `startDate`     | LocalDate        | ✔        | Ngày bắt đầu |
| `endDate`       | LocalDate?       | ✖        | Ngày kết thúc dự kiến (nếu có) |
| `status`        | SavingGoalStatus | ✔        | IN_PROGRESS/ACHIEVED/CANCELLED |
| `description`   | String?          | ✖        | Mô tả thêm |
| `transactions`  | List<Transaction>| ✖        | Danh sách giao dịch góp (mapped) |
| `histories`     | List<SavingHistory>| ✖      | Lịch sử tương tác |
| `createdAt`     | Instant          | ✔        | Ngày tạo |
| `updatedAt`     | Instant          | ✔        | Ngày cập nhật |

**Rule:**

- Khi góp thêm tiền:
  - `currentAmount = currentAmount + amount`.
  - Khi `currentAmount >= targetAmount` → status = `ACHIEVED`.
- Khi `CANCELLED` → không cho góp thêm.

---

### 3. Entity: SavingHistory

**Ý nghĩa:** Log lại mọi hành động liên quan tới SavingGoal.

| Field       | Kiểu             | Bắt buộc | Mô tả |
|-------------|------------------|----------|------|
| `id`        | Long             | ✔        | Khóa chính |
| `savingGoal`| SavingGoal       | ✔        | Mục tiêu liên quan |
| `user`      | User             | ✔        | Ai thực hiện hành động |
| `action`    | String           | ✔        | "Deposit", "Update goal", "Achieved", "Cancel", … |
| `amount`    | Money/BigDecimal?| ✖        | Số tiền của hành động (nếu có) |
| `totalAfter`| Money/BigDecimal?| ✖        | Tổng số sau khi hành động (nếu có) |
| `createdAt` | Instant          | ✔        | Thời điểm diễn ra |

---

### 4. Use case: Saving

- `createSavingGoal`
- `getSavingGoalDetail`
- `listSavingGoals`
- `updateSavingGoal`
- `deleteSavingGoal` / `cancelSavingGoal`
- `depositToSavingGoal` (góp tiền)
- `getHistoryForSavingGoal`
- Analytics:
  - `getMonthlyReportForSaving`
  - `getTopGoals`
  - `getFailedGoals` (hết hạn nhưng không đạt)
  - `getProgress(goalId)`
  - `getSummary(userId)`
  - `getTrend(userId)`


---

## VII. NOTIFICATION

### 1. Enum: NotificationStatus

- `PENDING`
- `SENT`
- `FAILED`

### 2. Enum: NotificationType (gợi ý)

- `TRANSACTION_CREATED`
- `SAVING_GOAL_ACHIEVED`
- `RECURRING_POSTED`
- `BUDGET_WARNING`
- `SYSTEM_MESSAGE`

---

### 3. Entity: Notification

| Field       | Kiểu              | Bắt buộc | Mô tả |
|-------------|-------------------|----------|------|
| `id`        | Long              | ✔        | Khóa chính |
| `user`      | User              | ✔        | Ai nhận thông báo |
| `type`      | NotificationType  | ✔        | Loại thông báo |
| `title`     | String            | ✔        | Tiêu đề |
| `body`      | String            | ✔        | Nội dung chính |
| `payload`   | String?           | ✖        | JSON string chứa data bổ sung (link, id…) |
| `status`    | NotificationStatus| ✔        | PENDING/SENT/FAILED |
| `attempts`  | int               | ✔        | Số lần cố gửi |
| `isRead`    | boolean           | ✔        | User đã đọc chưa |
| `createdAt` | Instant           | ✔        | Ngày tạo |
| `sentAt`    | Instant?          | ✖        | Thời điểm gửi (nếu SENT) |
| `readAt`    | Instant?          | ✖        | Thời điểm đọc (nếu isRead = true) |

---

### 4. Use case: Notification

- `getNotificationsForCurrentUser(paging)`
- `markNotificationAsRead(notificationId)`
- `markAllNotificationsAsRead`
- (backend) `sendNotification(notification)`  
  - handler sẽ quyết định gửi qua **Email**, **WebSocket**, … dựa trên `notificationChannel` của user.

---

## VIII. Business Rules Cross-cutting (tổng hợp nhanh)

- Mọi entity liên quan tiền đều dùng `Money` hoặc `BigDecimal` với cùng **currency** cho 1 account.  
- Mọi thao tác đọc/ghi data đều phải đảm bảo `user` hiện tại chỉ truy cập được **dữ liệu của chính mình**.  
- Các use case analytics (total, breakdown, chart) thường chạy trên **Transaction** kết hợp `UserCategory`, `Account`, `SavingGoal`.

