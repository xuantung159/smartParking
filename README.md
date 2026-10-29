# Smart Parking

## 1. Bài toán

Bãi xe có một số chỗ cố định, mỗi chỗ chỉ dành cho một loại xe (MOTORBIKE hoặc CAR). Có hai kiểu khách:

- Khách đặt trước: tra chỗ trống theo khung giờ, đặt một chỗ cụ thể, nhận mã đặt chỗ. Đến bãi thì đưa mã cho nhân viên.
- Khách vãng lai: đến thẳng bãi, nhân viên nhập biển số, hệ thống tự chọn một chỗ còn trống.

Khi xe ra, máy chủ ghi giờ ra và tính phí: xe máy 5.000đ/giờ, ô tô 20.000đ/giờ, làm tròn lên theo giờ, tối thiểu 1 giờ (gửi 61 phút tính 2 giờ).

Có 3 vai trò:

| Vai trò | Được làm gì |
|---|---|
| Chưa đăng nhập | Tra chỗ trống, đăng ký, đăng nhập |
| CUSTOMER | Đặt chỗ, xem và hủy lịch đặt của chính mình |
| STAFF | Check-in, check-out, xem/hủy lượt gửi; xem và hủy mọi lịch đặt |
| ADMIN | Mọi quyền của STAFF, cộng thêm tạo tài khoản và quản lý chỗ đỗ |

Khách tự đăng ký thì luôn là CUSTOMER. Tài khoản STAFF/ADMIN chỉ ADMIN tạo được.

### Vòng đời trạng thái

- Chỗ đỗ: `AVAILABLE` → `OCCUPIED` (có xe) → `AVAILABLE`. Admin ngừng dùng thì thành `DISABLED`, không xóa bản ghi.
- Lịch đặt: `ACTIVE` → `USED` (đã check-in) hoặc `CANCELLED`.
- Lượt gửi: `ACTIVE` → `COMPLETED` (đã check-out) hoặc `CANCELLED` (nhân viên nhập nhầm).

Check-in có mã đặt chỗ sẽ đụng tới cả ba bảng: tạo lượt gửi, đổi chỗ sang OCCUPIED, đổi lịch đặt sang USED. Ba việc này nằm chung một transaction, lỗi giữa chừng thì rollback hết.

### Các quy tắc

- Khoảng thời gian đặt là `[startTime, endTime)`. Lịch 09:00–11:00 chặn 10:00–12:00 nhưng không chặn 11:00–13:00.
- Chỉ lịch `ACTIVE` mới chặn giờ. Lịch phải ở tương lai và startTime < endTime.
- Chỉ check-in được trong khung startTime ≤ now < endTime, chưa có thời gian ân hạn. Lịch quá giờ vẫn để ACTIVE nhưng không dùng được.
- Khi cấp chỗ cho khách vãng lai, yêu cầu nghiệp vụ là không ảnh hưởng lịch đặt trước. Việc chọn chỗ và kiểm tra lại lịch trong cùng giao dịch là điểm cần rà soát ở phase 2.
- Một biển số chỉ có một lượt gửi ACTIVE; một mã đặt chỗ chỉ dùng một lần.
- Biển số được trim, viết hoa, tối đa 20 ký tự.

## 2. Công nghệ

| Thành phần | Dùng gì |
|---|---|
| Ngôn ngữ / framework | Java 17, Spring Boot 3.5 |
| Web, validate | spring-boot-starter-web, validation |
| Bảo mật | Spring Security + JWT (jjwt 0.12), mật khẩu băm BCrypt |
| Dữ liệu | Spring Data JPA (Hibernate), MySQL 8.4 |
| Tài liệu API | springdoc-openapi (Swagger UI) |
| Build / chạy | Maven wrapper, Docker, Docker Compose |
| CI | GitHub Actions: `mvn verify` và build Docker image mỗi lần push/PR vào main |

## 3. Tổ chức mã nguồn

```
src/main/java/com/nhom/parking/
├── auth/           đăng nhập, đăng ký, /me
├── user/           ADMIN tạo và liệt kê tài khoản
├── slot/           tạo và ngừng dùng chỗ đỗ
├── availability/   tra chỗ trống theo loại xe + khung giờ
├── reservation/    đặt, xem, hủy lịch; dùng lịch khi check-in
├── session/        check-in, check-out, tính phí
└── common/         security (JWT), config, exception, PageResponse, enum
src/main/resources/
├── application.yml
└── db/schema.sql, db/data.sql   tạo bảng + dữ liệu mẫu
```

Các phần nghiệp vụ được chia theo module

Luồng xử lý một request: 
- 1: bộ lọc JWT xác thực người gọi 
- 2: controller nhận dữ liệu và kiểm quyền 
- 3: service xử lý nghiệp vụ 
- 4: repository đọc/ghi MySQL.
  (1 --> 2 --> 3 --> 4)

Xử lý dữ liệu vào/ra và exception:
- DTO dùng cho dữ liệu vào/ra, mapper chuyển giữa entity và DTO. 
- Lỗi nghiệp vụ được gom về `common/exception`, trả theo dạng ErrorCode.<Lỗi> kèm mã lỗi.

Các thao tác thay đổi dữ liệu dùng `@Transactional` ở service. 

Dữ liệu chính gồm bốn bảng:

| Bảng | Nội dung |
|---|---|
| `users` | Tài khoản, mật khẩu băm và vai trò |
| `parking_slots` | Mã chỗ, loại xe và trạng thái thực tế |
| `reservations` | Người đặt, chỗ đỗ, biển số và khoảng thời gian giữ chỗ |
| `parking_sessions` | Lượt xe vào/ra, lịch đặt liên quan nếu có và phí gửi |

Schema và dữ liệu mẫu nằm trong `src/main/resources/db`. 

## 4. Chạy và deploy

### Chạy bằng Docker Compose

Cần Docker Desktop với Linux containers. Tại thư mục gốc dự án, tạo cấu hình:

```powershell
Copy-Item .env.example .env
```

Điền `DB_PASSWORD` và `MYSQL_ROOT_PASSWORD` trong `.env`, sau đó chạy:

```powershell
docker compose up -d --build --wait
```

Compose dựng 2 container: 
- `db` (MySQL 8.4, dữ liệu nằm trong volume `mysql_data`)
- `app`. App đợi MySQL healthy mới khởi động, rồi tự có healthcheck qua `/actuator/health`. 
- Dockerfile build 2 bước: image Maven để đóng gói jar, sau đó copy sang image JRE 17 gọn hơn và chạy bằng user không phải root. Cổng app chỉ bind vào `127.0.0.1`, đổi bằng `APP_PORT`.

Sau khi chạy:

- Swagger: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health
- Tài khoản mẫu: `admin`, `staff`, `customer`, mật khẩu đều là `123456`.

```powershell
docker compose logs -f app   # xem log
docker compose down          # tắt, giữ dữ liệu
docker compose down -v       # tắt và xóa luôn DB
```

### Build và chạy riêng backend

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/parking'
$env:DB_USERNAME = 'parking'
$env:DB_PASSWORD = '<mat-khau-db>'
.\mvnw.cmd spring-boot:run
```

Build và chạy kiểm thử bằng Maven wrapper:

```powershell
.\mvnw.cmd verify
```
### Cấu hình

| Biến | Ý nghĩa |
|---|---|
| `APP_PORT` | Cổng máy host khi chạy Compose, mặc định 8080 |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Kết nối DB khi chạy backend; Compose tự đặt URL và username cho app |
| `MYSQL_ROOT_PASSWORD` | Mật khẩu root khi khởi tạo container MySQL |
| `SERVER_PORT` | Cổng backend khi chạy riêng, mặc định 8080 |
| `JWT_SECRET` | Khóa ký JWT, tối thiểu 32 ký tự; nếu bỏ trống, app sinh khóa mới mỗi lần khởi động |
| `JWT_EXPIRES_IN` | Thời hạn token tính bằng giây, mặc định 3600 |

## 5. API

| Method | Endpoint | Quyền |
|---|---|---|
| POST | /api/auth/register, /api/auth/login | Công khai |
| GET | /api/auth/me | Đã đăng nhập |
| POST, GET | /api/users | ADMIN |
| POST | /api/slots | ADMIN |
| DELETE | /api/slots/{id} | ADMIN |
| GET | /api/availability?vehicleType=&startTime=&endTime= | Công khai |
| POST | /api/reservations | Đã đăng nhập |
| GET | /api/reservations/me | Đã đăng nhập |
| GET, DELETE | /api/reservations/{id} | Chủ lịch, STAFF, ADMIN |
| POST | /api/sessions/check-in | STAFF, ADMIN |
| POST | /api/sessions/{id}/check-out | STAFF, ADMIN |
| GET | /api/sessions, /api/sessions/{id} | STAFF, ADMIN |
| DELETE | /api/sessions/{id} | STAFF, ADMIN |

Mã lỗi:
- 400 dữ liệu sai
- 401 chưa/sai đăng nhập
- 403 sai quyền
- 404 không thấy
- 409 trùng lịch, hết chỗ, xe đang gửi hoặc trạng thái không cho phép.

Đặc tả chi tiết (request, response, mã lỗi):

- [docs/openapi.json](docs/openapi.json): OpenAPI 3, mở bằng [Swagger Editor](https://editor.swagger.io) hoặc trang [docs/index.html](docs/index.html) (bản GitHub Pages: https://xuantung159.github.io/smartParking/).
- [docs/postman/](docs/postman/): collection + environment để import vào Postman.
- Khi app đang chạy, `/v3/api-docs` trả đặc tả sinh trực tiếp từ code, đây là bản đúng nhất.

## 6. Các điểm cần tiếp tục ở phase 2

- Nhiều khách cùng đặt một chỗ; đặt chỗ đồng thời với nhận xe vãng lai hoặc ngừng sử dụng chỗ. Kiểm tra lại lịch sau khi khóa chỗ, tránh cấp chỗ dựa trên kết quả tra cứu đã cũ.
- Người dùng gửi lại yêu cầu vì mất phản hồi. Cần xác định cách nhận biết yêu cầu lặp, nhất là đặt chỗ, nhận xe và trả xe.
- Lỗi giữa giao dịch, chờ khóa và deadlock. Kiểm tra rollback, thứ tự lấy khóa và phản hồi lỗi để người dùng biết cách xử lý tiếp.
- Khách không đến, đến muộn hoặc xe ở quá giờ. Cần chốt thời gian ân hạn, cách hết hạn lịch và cách xử lý khi ảnh hưởng người đặt tiếp theo.
- Dữ liệu và lượng truy cập tăng. Đo độ trễ, thông lượng, tỷ lệ lỗi và kiểm tra dữ liệu sau mỗi lần chạy; từ đó chọn cách cải tiến truy vấn, chỉ mục, phân trang hoặc cache.
