# Smart Parking

Backend quản lý bãi đỗ xe dùng Spring Boot, Java 17 và MySQL.

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

## Phạm vi triển khai

Cấu hình này phục vụ chạy local/demo. Project hiện cho phép truy cập tất cả API và có tài khoản mẫu `admin`, `staff`, `customer` với mật khẩu `123456`; cần hoàn thiện xác thực, phân quyền và bỏ dữ liệu mẫu trước khi mở dịch vụ ra Internet.
