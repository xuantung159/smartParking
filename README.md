# Smart Parking

Smart Parking là backend quản lý bãi đỗ xe máy và ô tô, được xây dựng trong môn Kiến trúc phần mềm. Khách hàng có thể tra cứu và đặt chỗ theo khoảng thời gian; nhân viên quản lý lượt gửi xe và tính phí; quản trị viên quản lý tài khoản và chỗ đỗ.

Phần nhóm tập trung tìm hiểu là xử lý đặt chỗ online khi nhiều yêu cầu xảy ra cùng lúc. Hai khách có thể cùng nhìn thấy một chỗ trống, nhưng khi xác nhận thì chỉ một người được đặt thành công nếu khung giờ bị trùng. Hệ thống cũng cần xử lý trường hợp yêu cầu gửi lặp hoặc chỗ đã được người khác đặt sau thời điểm tra cứu.

## Chức năng chính

| Nhóm chức năng | Nội dung |
|---|---|
| Đăng nhập | Kiểm tra tài khoản, cấp JWT để gọi API cần xác thực |
| Quản lý tài khoản | Tạo tài khoản, xem danh sách và lọc theo vai trò |
| Quản lý chỗ đỗ | Tạo chỗ theo loại xe, ngừng sử dụng chỗ bằng cách đổi trạng thái |
| Tra cứu | Tìm chỗ phù hợp với loại xe và khoảng thời gian yêu cầu |
| Đặt chỗ | Tạo, xem và hủy đặt chỗ của bản thân; kiểm tra trùng lịch |
| Lượt gửi xe | Ghi nhận xe vào, xe ra, tính phí, xem và hủy lượt nhập nhầm |

| Vai trò | Phạm vi sử dụng theo thiết kế |
|---|---|
| Công khai | Đăng nhập và tra cứu chỗ trống |
| `ADMIN` | Quản lý tài khoản và chỗ đỗ |
| `CUSTOMER` | Tạo, xem, hủy đặt chỗ của mình |
| `STAFF` | Quản lý lượt gửi xe và tính phí |

## Luồng hoạt động

### Đặt chỗ online

1. Khách hàng đăng nhập, chọn loại xe, giờ bắt đầu và giờ kết thúc.
2. Hệ thống tìm các chỗ phù hợp, loại trừ chỗ không sử dụng được và các lịch đặt bị trùng.
3. Khách chọn một chỗ, nhập biển số và xác nhận đặt.
4. Hệ thống kiểm tra lại điều kiện trong giao dịch trước khi lưu. Nếu có nhiều yêu cầu cùng đặt một chỗ trong các khung giờ giao nhau, chỉ yêu cầu hợp lệ đầu tiên được chấp nhận.
5. Đặt thành công thì hệ thống trả mã đặt chỗ. Nếu chỗ không còn phù hợp, khách nhận thông báo để chọn lại.
6. Khách có thể xem lại hoặc hủy đặt chỗ chưa sử dụng. Hủy bằng cách đổi trạng thái để giữ lịch sử.

Ví dụ, khách A đặt chỗ `C-01` từ 9h đến 11h thì khách B không thể đặt chỗ đó từ 10h đến 12h. Theo quy ước đề xuất `[startTime, endTime)`, B có thể đặt từ 11h đến 12h vì hai khoảng liền kề không giao nhau.

### Xe vào và xe ra

1. Nhân viên ghi nhận biển số, loại xe và mã đặt chỗ nếu có.
2. Hệ thống kiểm tra xe có đang gửi trong bãi hay không, kiểm tra đặt chỗ và chỗ đỗ phù hợp.
3. Khi xe vào, hệ thống tạo lượt gửi `ACTIVE`, đổi chỗ thành `OCCUPIED` và đánh dấu đặt chỗ `USED` nếu sử dụng đặt trước. Các thay đổi phải nằm trong cùng một giao dịch.
4. Khi xe ra, hệ thống ghi giờ ra, tính phí theo loại xe và thời gian gửi, chuyển lượt gửi thành `COMPLETED` và trả chỗ về `AVAILABLE`.
5. Nếu nhân viên nhập nhầm một lượt chưa kết thúc, có thể hủy lượt đó và trả lại chỗ.

Biểu phí, cách làm tròn thời gian và chính sách khách đến sớm, đến muộn hoặc gửi quá giờ cần được thống nhất khi hoàn thiện baseline.

### Các tình huống cần kiểm thử

- Hai khách đặt cùng một chỗ trong khoảng thời gian giao nhau.
- Nhiều khách cùng yêu cầu khi chỉ còn một chỗ phù hợp.
- Khách gửi lại yêu cầu do bấm nhiều lần hoặc mất phản hồi mạng.
- Kết quả tra cứu còn chỗ nhưng đến lúc xác nhận thì chỗ đã được đặt.
- Một đặt chỗ được hủy đúng lúc có yêu cầu đặt mới cho cùng chỗ.

Đây là các tình huống dùng để kiểm tra tính đúng đắn của baseline và làm cơ sở đánh giá cải tiến ở pha 2; cơ chế chống yêu cầu lặp chưa được triển khai.

## Kiến trúc và công nghệ

Baseline sử dụng kiến trúc nguyên khối: một dự án Maven, một ứng dụng Spring Boot và một cơ sở dữ liệu MySQL. Mã nguồn chia theo module chức năng; thiết kế mỗi module gồm controller, service, repository, entity, DTO và mapper khi cần.

Luồng xử lý dự kiến:

```text
Client → Kiểm tra JWT và quyền → Controller → Service → Repository → MySQL
```

Controller nhận và kiểm tra định dạng dữ liệu. Service xử lý nghiệp vụ và giao dịch. Repository truy vấn dữ liệu. Các module trao đổi qua service; phần dùng chung như cấu hình và xử lý lỗi nằm trong `common`.

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17 |
| Backend | Spring Boot 3.5, Spring Web |
| Dữ liệu | Spring Data JPA, Hibernate, MySQL 8.4 trong Compose |
| Bảo mật | Spring Security, BCrypt; JWT dự kiến dùng JJWT |
| Tài liệu API | Springdoc OpenAPI, Swagger UI |
| Build và đóng gói | Maven, Docker, Docker Compose |

### Cấu trúc mã nguồn

```text
src/main/
├── java/com/nhom/parking/
│   ├── auth/           Đăng nhập
│   ├── user/           Tài khoản
│   ├── slot/           Chỗ đỗ
│   ├── availability/   Tra cứu chỗ trống
│   ├── reservation/    Đặt chỗ trước
│   ├── session/        Lượt gửi xe và tính phí
│   ├── common/         Cấu hình, enum và xử lý lỗi chung
│   └── ParkingApplication.java
└── resources/
    ├── application.yml
    └── db/
        ├── schema.sql
        └── data.sql
```

### Dữ liệu chính

| Bảng | Nội dung | Trạng thái |
|---|---|---|
| `users` | Tài khoản, mật khẩu băm, họ tên và vai trò | — |
| `parking_slots` | Mã chỗ, loại xe và tình trạng chỗ đỗ | `AVAILABLE`, `OCCUPIED`, `DISABLED` |
| `reservations` | Khách đặt, chỗ đỗ, biển số và khoảng thời gian | `ACTIVE`, `CANCELLED`, `USED` |
| `parking_sessions` | Chỗ đỗ, đặt chỗ liên quan nếu có, giờ vào/ra và phí | `ACTIVE`, `COMPLETED`, `CANCELLED` |

Một tài khoản có nhiều đặt chỗ; một chỗ đỗ có nhiều đặt chỗ và lượt gửi theo thời gian. Lượt gửi có thể liên kết với đặt chỗ qua `reservation_id`.

## Danh sách endpoint

Base URL mặc định: `http://localhost:8080`.

Quyền trong bảng là **quyền mục tiêu**, chưa được áp dụng trong cấu hình hiện tại. Các endpoint ghi `501` chưa nhận DTO hoặc thực hiện nghiệp vụ. Các endpoint ghi “Chưa đăng ký” chưa gọi được theo chức năng thiết kế.

| Phương thức | Endpoint | Chức năng | Quyền mục tiêu | Hiện trạng |
|---|---|---|---|---|
| POST | `/api/auth/login` | Đăng nhập, cấp JWT | Công khai | `501` |
| POST | `/api/users` | Tạo tài khoản | ADMIN | `501` |
| GET | `/api/users` | Danh sách tài khoản, lọc vai trò | ADMIN | `501` |
| POST | `/api/slots` | Tạo chỗ đỗ | ADMIN | `501` |
| DELETE | `/api/slots/{id}` | Ngừng sử dụng chỗ đỗ | ADMIN | `501` |
| GET | `/api/availability` | Tra cứu theo loại xe và khung giờ | Công khai | `501` |
| POST | `/api/reservations` | Tạo đặt chỗ | CUSTOMER | Chưa đăng ký |
| GET | `/api/reservations/me` | Danh sách đặt chỗ của bản thân | CUSTOMER | Chưa đăng ký |
| GET | `/api/reservations/{id}` | Chi tiết đặt chỗ của bản thân | CUSTOMER | Chưa đăng ký |
| DELETE | `/api/reservations/{id}` | Hủy đặt chỗ của bản thân | CUSTOMER | Chưa đăng ký |
| POST | `/api/sessions/check-in` | Ghi nhận xe vào | STAFF | `501` |
| POST | `/api/sessions/{id}/check-out` | Ghi nhận xe ra, tính phí | STAFF | `501` |
| GET | `/api/sessions` | Danh sách lượt gửi, lọc trạng thái | STAFF | `501` |
| GET | `/api/sessions/{id}` | Chi tiết lượt gửi | STAFF | `501` |
| DELETE | `/api/sessions/{id}` | Hủy lượt gửi nhập nhầm | STAFF | `501` |

### Quy ước phản hồi mục tiêu

API trao đổi dữ liệu JSON. Khi JWT được triển khai, API cần đăng nhập sử dụng header `Authorization: Bearer <token>`.

| HTTP status | Ý nghĩa |
|---|---|
| `200` | Truy vấn, đăng nhập hoặc xử lý thành công |
| `201` | Tạo mới thành công |
| `204` | Hủy hoặc ngừng sử dụng thành công, không có body |
| `400` | Dữ liệu đầu vào không hợp lệ |
| `401` | Chưa đăng nhập hoặc thông tin xác thực không hợp lệ |
| `403` | Không đủ quyền hoặc truy cập đặt chỗ của người khác |
| `404` | Không tìm thấy tài nguyên |
| `409` | Xung đột nghiệp vụ như trùng lịch, hết chỗ hoặc xe đang gửi |

Handler hiện có hỗ trợ lỗi nghiệp vụ dạng `ProblemDetail`, kèm trường `code`. Ví dụ lỗi trùng lịch khi nghiệp vụ được triển khai:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Chỗ đỗ đã được đặt trong khung giờ này",
  "code": "RESERVATION_TIME_CONFLICT"
}
```

Request/response đề xuất, các quy tắc chi tiết và kịch bản review nằm trong [tài liệu chuẩn bị review](CHUAN-BI-REVIEW.md). Swagger hiện phản ánh các mapping trong mã nguồn, chưa phải hợp đồng hoàn chỉnh của 15 API.

## Chạy bằng Docker

Cài Docker Desktop và bật Linux containers. Tại thư mục project:

```powershell
Copy-Item .env.example .env
```

Đổi `DB_PASSWORD` và `MYSQL_ROOT_PASSWORD` trong `.env`, sau đó chạy:

```powershell
docker compose up -d --build --wait
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health

Health báo ứng dụng sẵn sàng không có nghĩa các chức năng nghiệp vụ đã được triển khai.

Đổi `APP_PORT` trong `.env` nếu cổng 8080 đang được dùng. Ứng dụng chỉ mở cổng trên localhost; MySQL được truy cập qua mạng nội bộ Compose.

Image được build bằng Maven và chạy trên Java 17 với user không có quyền root. Compose đợi MySQL sẵn sàng rồi mới chạy backend. Ứng dụng tự tạo bảng và nạp dữ liệu mẫu từ `src/main/resources/db/` khi khởi động.

## Quản lý container

```powershell
docker compose ps
docker compose logs -f app
docker compose down
```

Database nằm trong volume `mysql_data` và vẫn được giữ khi chạy `docker compose down`. Lệnh `docker compose down -v` xóa toàn bộ dữ liệu database.

Mật khẩu MySQL trong `.env` chỉ thiết lập khi volume được tạo lần đầu. Với database đã có dữ liệu, cần đổi mật khẩu trong MySQL trước khi cập nhật `.env`.

## Cấu hình và dữ liệu mẫu

| Biến | Mục đích |
|---|---|
| `APP_PORT` | Cổng truy cập backend trên máy khi chạy Compose; mặc định 8080 |
| `DB_PASSWORD` | Mật khẩu tài khoản MySQL `parking` |
| `MYSQL_ROOT_PASSWORD` | Mật khẩu root để khởi tạo MySQL |
| `DB_URL` | JDBC URL; Compose tự đặt để kết nối service `db` |
| `DB_USERNAME` | Tài khoản kết nối DB; Compose đặt là `parking` |
| `SERVER_PORT` | Cổng ứng dụng bên trong container; Compose đặt là 8080 |

Hibernate đang dùng `ddl-auto: validate`, chỉ đối chiếu entity với bảng. Khi đổi entity, cần cập nhật `schema.sql` và có phương án cập nhật database đã tồn tại; `CREATE TABLE IF NOT EXISTS` không tự sửa cấu trúc bảng cũ.

Script dữ liệu mẫu dùng `INSERT IGNORE`, gồm ba tài khoản dưới đây và sáu chỗ đỗ `M-01`–`M-03`, `C-01`–`C-03`; `C-03` ngừng sử dụng. Các tài khoản được nạp vào DB nhưng chức năng đăng nhập hiện chưa hoạt động.

| Tài khoản | Mật khẩu demo | Vai trò |
|---|---|---|
| `admin` | `123456` | ADMIN |
| `staff` | `123456` | STAFF |
| `customer` | `123456` | CUSTOMER |

## Hướng phát triển pha 2

Sau khi baseline chạy được, nhóm sẽ kiểm thử các yêu cầu đặt chỗ đồng thời, yêu cầu gửi lặp và tải tra cứu. Từ kết quả đó, nhóm chọn vấn đề để cải tiến cách xử lý giao dịch, truy vấn dữ liệu hoặc ranh giới giữa các module.

Việc đánh giá cần so sánh trước và sau trên cùng điều kiện: số đặt chỗ trùng, thời gian phản hồi, số yêu cầu xử lý được và tài nguyên sử dụng. Hiện chưa có số liệu để kết luận về hiệu năng baseline.

## Phạm vi triển khai

Cấu hình này phục vụ chạy local/demo. Project hiện cho phép truy cập tất cả API và có tài khoản mẫu `admin`, `staff`, `customer` với mật khẩu `123456`; cần hoàn thiện xác thực, phân quyền và bỏ dữ liệu mẫu trước khi mở dịch vụ ra Internet.
