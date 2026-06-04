# Sales Management System

Ứng dụng quản lý bán hàng PBL3, xây dựng bằng Java 21, JavaFX, Spring Boot, Spring Data JPA, MySQL/TiDB và Flyway Java migrations.

## Chức năng chính

- Quản lý sản phẩm, danh mục, thương hiệu, xuất xứ, đơn vị tính và nhà cung cấp.
- Bán hàng POS, tạo đơn hàng, hoàn trả, lịch sử đơn hàng và ca bán hàng.
- Quản lý nhập hàng, tồn kho, kiểm kê, cảnh báo và báo cáo vận hành.
- Quản lý khách hàng, khuyến mãi, chi phí, tài khoản người dùng và nhật ký kiểm toán.
- Tích hợp thanh toán QR qua SePay/VietQR khi cấu hình API token.
- Hỗ trợ desktop client theo workspace tenant và provisioning API cho bản phát hành.

## Yêu cầu môi trường

- JDK 21 trở lên.
- Maven Wrapper đã có sẵn trong repo (`./mvnw` hoặc `mvnw.cmd`).
- MySQL 8.x hoặc TiDB dùng giao thức MySQL.
- Git và terminal.

Kiểm tra nhanh:

```bash
java -version
./mvnw -version
mysql --version
```

## Cài đặt local

1. Clone repo và vào thư mục dự án:

```bash
git clone <repository-url>
cd PBL3-Project
```

2. Tạo database local:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS pbl3_project CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

3. Cấu hình tài khoản database.

Mặc định ứng dụng dùng:

```text
spring.datasource.url=jdbc:mysql://localhost:3306/pbl3_project?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
```

Nếu MySQL có mật khẩu, truyền qua biến môi trường:

```bash
export DB_USERNAME=root
export DB_PASSWORD=<mat-khau-mysql>
```

Trên Windows PowerShell:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<mat-khau-mysql>"
```

4. Chạy migration tạo/cập nhật schema.

Dự án dùng Flyway Java migrations trong `src/main/java/db/migration`. Ứng dụng desktop thông thường không tự chạy migration, vì vậy hãy chạy lệnh này trước khi mở app lần đầu hoặc sau khi pull code có thay đổi schema:

```bash
./mvnw spring-boot:run -Dspring-boot.run.main-class=com.pbl3.project.pbl3_project.DatabaseMigrationApplication
```

5. Chạy ứng dụng desktop:

```bash
./mvnw javafx:run
```

Khi database trống, ứng dụng sẽ tự tạo tài khoản quản trị mặc định:

```text
Username: admin
Password: admin
```

Nên đổi mật khẩu ngay sau khi đăng nhập lần đầu.

## Chạy với dữ liệu demo

Database demo mặc định là `pbl3_project_demo`. Tạo và seed dữ liệu demo:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS pbl3_project_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
./mvnw spring-boot:run -Dspring-boot.run.main-class=com.pbl3.project.pbl3_project.DemoDataSeedApplication
```

Các tài khoản demo:

```text
admin/admin
manager/manager
staff/staff
```

Sau khi seed, chạy app và trỏ datasource sang database demo:

```bash
./mvnw package -DskipTests
java -jar target/pbl3-project-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url="jdbc:mysql://localhost:3306/pbl3_project_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
  --spring.datasource.username="${DB_USERNAME:-root}" \
  --spring.datasource.password="${DB_PASSWORD:-}"
```

Không chạy desktop app với profile `demo`; entry point JavaFX sẽ chặn các profile `demo`, `migration` và `provisioning`.

## Chạy test và build

Chạy toàn bộ test:

```bash
./mvnw test
```

Build jar:

```bash
./mvnw package
```

Build nhanh bỏ qua test:

```bash
./mvnw package -DskipTests
```

Jar sau khi build nằm ở:

```text
target/pbl3-project-0.0.1-SNAPSHOT.jar
```

## Cấu hình thanh toán QR

Các biến môi trường liên quan SePay/VietQR:

```text
SEPAY_BASE_URL=https://userapi.sepay.vn/v2
SEPAY_QR_BASE_URL=https://qr.sepay.vn
SEPAY_API_TOKEN=<token-sepay>
SEPAY_WEBHOOK_API_KEY=<webhook-key>
SEPAY_BANK=<ma-ngan-hang>
SEPAY_ACCOUNT_NUMBER=<so-tai-khoan>
SEPAY_QR_EXPIRE_SECONDS=300
APP_PUBLIC_BASE_URL=http://localhost:8080
```

Nếu không cấu hình token/ngân hàng, các chức năng còn lại vẫn chạy, nhưng luồng thanh toán QR sẽ không hoạt động đầy đủ.

## REST API

Khi chạy bằng cấu hình mặc định, Spring Boot web server cũng khởi động. Một số endpoint chính:

```text
POST /api/auth/login
GET  /api/products
POST /api/orders
GET  /api/orders
GET  /api/reports/daily
GET  /api/reports/monthly
POST /api/qr-payments
POST /api/payments/sepay/webhook
```

Đăng nhập API bằng:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

Các API nghiệp vụ yêu cầu header `Authorization` với session token trả về từ endpoint đăng nhập.

## Provisioning API và desktop release

Provisioning API dùng cho mô hình nhiều tenant và onboarding desktop client. Build API:

```bash
./mvnw -Pprovisioning-api -DskipTests package
```

Chạy local:

```bash
SPRING_PROFILES_ACTIVE=provisioning \
PROVISIONING_API_KEY=<strong-random-key> \
TIDB_JDBC_URL=<registry-jdbc-url> \
TIDB_USERNAME=<registry-user> \
TIDB_PASSWORD=<registry-password> \
TIDB_ADMIN_JDBC_URL=<admin-jdbc-url> \
TIDB_ADMIN_USERNAME=<admin-user> \
TIDB_ADMIN_PASSWORD=<admin-password> \
./mvnw spring-boot:run -Dspring-boot.run.main-class=com.pbl3.project.pbl3_project.provisioning.TenantProvisioningApplication
```

Health check:

```text
GET /api/provisioning/health
```

Build desktop release jar:

```bash
./mvnw -Pdesktop-release -DskipTests package
```

Đóng gói DMG trên macOS:

```bash
PROVISIONING_API_BASE_URL=https://provisioning.example.com \
PROVISIONING_API_KEY=<client-provisioning-key> \
./scripts/package-macos-dmg.sh
```

Xem thêm hướng dẫn chi tiết ở `docs/deployment.md`.

## Cấu trúc thư mục

```text
src/main/java/com/pbl3/project/pbl3_project
  controller/       REST API controllers
  service/          Business logic
  repository/       Spring Data repositories
  entity/           JPA entities
  ui/               JavaFX UI
  provisioning/     Tenant provisioning API
src/main/java/db/migration
  V*__*.java        Flyway Java migrations
src/main/resources
  application*.properties, CSS, icon và tài nguyên UI
src/test/java
  Unit/integration tests
docs
  Tài liệu triển khai, kiến trúc, class diagram và database diagram
scripts
  Script seed demo, reset demo DB và đóng gói installer
```

## Lỗi thường gặp

- `Access denied for user`: kiểm tra `DB_USERNAME` và `DB_PASSWORD`.
- `Unknown database 'pbl3_project'`: tạo database bằng lệnh trong phần cài đặt.
- `Schema-validation: missing table/column`: chạy lại `DatabaseMigrationApplication`.
- JavaFX không mở cửa sổ: kiểm tra đang dùng JDK 21 đầy đủ, không phải JRE tối giản.
- Desktop app báo không được chạy profile `demo`/`migration`/`provisioning`: bỏ `SPRING_PROFILES_ACTIVE` hoặc chỉ dùng profile phù hợp với lệnh chuyên dụng.

## Ghi chú bảo mật

- Không commit mật khẩu MySQL/TiDB, SePay token hoặc provisioning key vào source code.
- Không đóng gói credential admin/root vào desktop client.
- Với production, cấu hình `APP_API_SESSION_SECRET` để session token ổn định và không bị forge sau khi restart.
