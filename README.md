# Smart Parking

Bài tập lớn môn Kiến trúc phần mềm: xây dựng backend quản lý bãi đỗ xe máy và ô tô. Khách hàng đặt chỗ trước, nhân viên ghi nhận xe vào/ra và tính phí, quản trị viên quản lý tài khoản và chỗ đỗ.



## Nghiệp vụ

| Vai trò | Công việc |
|---|---|
| Khách chưa đăng nhập | Tra cứu chỗ trống, đăng nhập |
| CUSTOMER | Đặt chỗ, xem và hủy lịch đặt của mình |
| STAFF | Ghi nhận xe vào/ra, xem lượt gửi, hủy lượt nhập nhầm |
| ADMIN | Tạo và xem tài khoản; tạo, ngừng sử dụng chỗ đỗ |

Tài khoản do ADMIN cấp. ADMIN không mặc nhiên có quyền của STAFF hoặc CUSTOMER.

### Đặt chỗ

1. Khách chọn loại xe và thời gian gửi, xem các chỗ phù hợp.
2. Đăng nhập, chọn chỗ và nhập biển số.
3. Khi xác nhận, hệ thống kiểm tra lại lịch rồi tạo đặt chỗ ACTIVE, trả mã đặt chỗ. Nếu chỗ vừa được người khác đặt thì trả 409 để khách chọn lại.
4. Khách xem lịch của mình hoặc hủy khi chưa sử dụng. Khi đến bãi, đưa mã cho nhân viên.

Tra cứu không giữ chỗ. Ví dụ C-01 đã được đặt 09:00–11:00 thì không nhận lịch 10:00–12:00, nhưng có thể nhận lịch bắt đầu từ 11:00.

### Xe vào và xe ra

1. Nhân viên nhập biển số, loại xe và mã đặt chỗ nếu có.
2. Có mã đặt trước: kiểm tra lịch còn hiệu lực, đúng xe và chỗ còn trống. Khách vãng lai được hệ thống chọn chỗ phù hợp.
3. Xe vào: tạo lượt gửi ACTIVE, đổi chỗ thành OCCUPIED; đặt chỗ chuyển USED nếu có.
4. Xe ra: máy chủ ghi giờ ra, tính phí, chuyển lượt gửi COMPLETED và trả chỗ về AVAILABLE.
5. Nếu nhập nhầm, nhân viên được hủy lượt ACTIVE sau khi xác minh xe không chiếm chỗ. Đặt chỗ đã USED không tự khôi phục.

Các cập nhật lượt gửi, chỗ đỗ và đặt chỗ phải nằm trong cùng giao dịch; có lỗi thì rollback toàn bộ.

### Quản lý bãi

ADMIN tạo tài khoản theo vai trò và khai báo chỗ theo loại xe MOTORBIKE/CAR. Chỗ mới có trạng thái AVAILABLE. Ngừng sử dụng chỗ là đổi sang DISABLED, không xóa lịch sử; từ chối nếu chỗ còn xe hoặc lịch đặt chưa kết thúc.

### Quy tắc cần thống nhất khi triển khai

- Thời gian đặt phải ở tương lai, startTime < endTime. Dùng khoảng [startTime, endTime), cho phép hai lịch liền nhau.
- Chỉ lịch ACTIVE chặn giờ đặt. Chỗ OCCUPIED hoặc DISABLED không nhận đặt mới.
- Một chỗ và một biển số chỉ có tối đa một lượt gửi ACTIVE; một đặt chỗ chỉ được dùng một lần.
- Biển số được bỏ khoảng trắng đầu/cuối, viết hoa, dài tối đa 20 ký tự.
- Đề xuất check-in trong khoảng startTime ≤ giờ hiện tại < endTime, chưa có thời gian ân hạn. Lịch quá giờ vẫn lưu ACTIVE nhưng không được check-in.
- Với khách vãng lai, đề xuất chỉ cấp chỗ trống không có lịch ACTIVE nào kết thúc sau thời điểm hiện tại, vì chưa biết giờ xe ra.
- Phí demo đề xuất: xe máy 5.000đ/giờ, ô tô 20.000đ/giờ; làm tròn lên, tối thiểu một giờ. Gửi 61 phút tính hai giờ. Chưa có bước xác nhận thu tiền.

## API

Base URL: http://localhost:8080. Dữ liệu JSON; API cần đăng nhập dùng header Authorization: Bearer <token>. Quyền trong bảng là quyền theo thiết kế, chưa áp dụng trong mã hiện tại.

| Method | Endpoint | Quyền | Nội dung |
|---|---|---|---|
| POST | /api/auth/login | Công khai | Đăng nhập bằng username, password |
| POST | /api/users | ADMIN | Tạo tài khoản: username, password, fullName, role |
| GET | /api/users | ADMIN | Danh sách tài khoản, lọc role |
| POST | /api/slots | ADMIN | Tạo chỗ: code, vehicleType |
| DELETE | /api/slots/{id} | ADMIN | Ngừng sử dụng chỗ |
| GET | /api/availability | Công khai | Tra cứu theo vehicleType, startTime, endTime |
| POST | /api/reservations | CUSTOMER | Đặt chỗ: slotId, licensePlate, startTime, endTime |
| GET | /api/reservations/me | CUSTOMER | Lịch đặt của mình, lọc status |
| GET | /api/reservations/{id} | CUSTOMER | Chi tiết lịch đặt của mình |
| DELETE | /api/reservations/{id} | CUSTOMER | Hủy đặt chỗ |
| POST | /api/sessions/check-in | STAFF | Xe vào: licensePlate, vehicleType, reservationCode tùy chọn |
| POST | /api/sessions/{id}/check-out | STAFF | Xe ra và tính phí |
| GET | /api/sessions | STAFF | Danh sách lượt gửi, lọc status |
| GET | /api/sessions/{id} | STAFF | Chi tiết lượt gửi |
| DELETE | /api/sessions/{id} | STAFF | Hủy lượt nhập nhầm |

Danh sách dự kiến dùng page từ 0, size mặc định 20, tối đa 100; trả items, page, size, totalElements. Thời gian có dạng 2026-10-21T09:00:00, thống nhất giờ Việt Nam. Giờ vào/ra, phí, trạng thái và userId do máy chủ xác định.

Ví dụ body đặt chỗ; slotId lấy từ kết quả tra cứu, thời gian phải còn ở tương lai:

~~~json
{
  "slotId": 1,
  "licensePlate": "59A-12345",
  "startTime": "2026-10-21T09:00:00",
  "endTime": "2026-10-21T11:00:00"
}
~~~

Tạo thành công trả 201 cùng thông tin đặt chỗ và mã để check-in. Đăng nhập trả 200 với accessToken và thông tin tài khoản; xe ra trả 200 với giờ ra, phí và trạng thái COMPLETED. Hủy hoặc ngừng sử dụng trả 204, không body.

| Mã HTTP | Ý nghĩa |
|---|---|
| 400 | Dữ liệu không hợp lệ |
| 401 | Sai thông tin đăng nhập hoặc token không hợp lệ |
| 403 | Sai quyền hoặc truy cập lịch đặt người khác |
| 404 | Không tìm thấy tài nguyên |
| 409 | Trùng lịch, hết chỗ, xe đang gửi hoặc trạng thái không cho phép |

Lỗi nghiệp vụ dùng ProblemDetail, có trường code như RESERVATION_TIME_CONFLICT hoặc VEHICLE_ALREADY_PARKED. Hủy lại bản ghi CANCELLED trả 204; hủy lịch USED hoặc lượt gửi COMPLETED trả 409.

Chi tiết request, response và mã lỗi: [OpenAPI](docs/openapi.json), [tài liệu review](CHUAN-BI-REVIEW.md). [Swagger trên web](https://xuantung159.github.io/smartParking/) cần bật GitHub Pages từ nhánh main, thư mục /docs; trang này mô tả API dự kiến và đã tắt gọi thử.

## Công nghệ và dữ liệu

Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security, MySQL 8.4, Maven và Docker Compose. Kiến trúc nguyên khối, chia module auth, user, slot, availability, reservation, session và common.

Luồng xử lý dự kiến: Client → JWT/phân quyền → Controller → Service → Repository → MySQL.

| Bảng | Lưu trữ | Trạng thái |
|---|---|---|
| users | Tài khoản, mật khẩu băm BCrypt, họ tên, vai trò | — |
| parking_slots | Mã chỗ và loại xe | AVAILABLE, OCCUPIED, DISABLED |
| reservations | Khách đặt, chỗ, biển số, khoảng thời gian | ACTIVE, CANCELLED, USED |
| parking_sessions | Chỗ, đặt chỗ nếu có, biển số, giờ vào/ra, phí | ACTIVE, COMPLETED, CANCELLED |

Một tài khoản có nhiều lịch đặt; một chỗ có nhiều lịch đặt và lượt gửi theo thời gian. Schema có khóa ngoại và mã duy nhất, nhưng chưa đủ để ngăn đặt trùng hoặc hai lượt ACTIVE khi có yêu cầu đồng thời.

## Chạy dự án

Cài Docker Desktop, bật Linux containers rồi tạo file cấu hình:

~~~powershell
Copy-Item .env.example .env
~~~

Đặt DB_PASSWORD và MYSQL_ROOT_PASSWORD trong .env, sau đó chạy:

~~~powershell
docker compose up -d --build --wait
~~~

- [Swagger local](http://localhost:8080/swagger-ui/index.html): các mapping hiện có.
- [Health](http://localhost:8080/actuator/health): kiểm tra ứng dụng.
- APP_PORT trong .env dùng để đổi cổng, mặc định 8080.

Compose tự cấu hình kết nối app–MySQL. Khi chạy riêng, dùng DB_URL, DB_USERNAME, DB_PASSWORD và SERVER_PORT theo application.yml. Bảng và dữ liệu mẫu nằm trong src/main/resources/db; Hibernate dùng ddl-auto: validate nên sửa entity phải cập nhật schema tương ứng. Script tạo bảng không tự nâng cấp cấu trúc bảng cũ.

Dữ liệu mẫu có ba tài khoản admin, staff, customer, cùng mật khẩu 123456; sáu chỗ M-01–M-03 và C-01–C-03, trong đó C-03 ngừng sử dụng. Đăng nhập hiện chưa hoạt động.

~~~powershell
docker compose ps
docker compose logs -f app
docker compose down
~~~

Dữ liệu được giữ trong volume mysql_data. Lệnh docker compose down -v xóa cả dữ liệu. Mật khẩu trong .env chỉ khởi tạo MySQL lần đầu; nếu volume đã có thì phải đổi mật khẩu trong DB tương ứng.

## Phần cần kiểm thử

- Đặt đồng thời cùng chỗ, hai lịch giao nhau và hai lịch liền kề.
- Check-in trùng biển số hoặc dùng cùng mã đặt chỗ hai lần.
- Tra cứu còn chỗ nhưng lúc xác nhận đã hết; gửi lại yêu cầu khi mất phản hồi.
- Sai vai trò, xem/hủy lịch người khác, hủy lượt đã kết thúc.
- Check-out đồng thời với hủy, tính phí ở mốc 60 và 61 phút.
- Lỗi giữa giao dịch phải rollback cả lượt gửi, chỗ và lịch đặt.

Sau khi hoàn thiện luồng cơ bản, nhóm sẽ đo tải tra cứu/đặt chỗ và so sánh các cách xử lý đồng thời ở pha 2. Hiện chưa có số liệu hiệu năng. Cấu hình này dùng cho local/demo; cần hoàn thiện xác thực, phân quyền và bỏ tài khoản mẫu trước khi triển khai công khai.
