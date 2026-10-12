-- Dữ liệu mẫu. INSERT IGNORE để chạy lại không bị trùng.
-- Ba tài khoản mẫu dùng chung mật khẩu 123456 (đã băm BCrypt).
INSERT IGNORE INTO users (id, username, password_hash, full_name, role) VALUES
    (1, 'admin',    '$2a$10$VNtYzmcRH0tCN92EP/B4g.EYP7N90scVPIcL9aVkTSbMkpMmubBfy', 'Quản trị viên', 'ADMIN'),
    (2, 'staff',    '$2a$10$VNtYzmcRH0tCN92EP/B4g.EYP7N90scVPIcL9aVkTSbMkpMmubBfy', 'Nhân viên bãi', 'STAFF'),
    (3, 'customer', '$2a$10$VNtYzmcRH0tCN92EP/B4g.EYP7N90scVPIcL9aVkTSbMkpMmubBfy', 'Khách hàng',    'CUSTOMER');

INSERT IGNORE INTO parking_slots (id, code, vehicle_type, status) VALUES
    (1, 'M-01', 'MOTORBIKE', 'AVAILABLE'),
    (2, 'M-02', 'MOTORBIKE', 'AVAILABLE'),
    (3, 'M-03', 'MOTORBIKE', 'AVAILABLE'),
    (4, 'C-01', 'CAR',       'AVAILABLE'),
    (5, 'C-02', 'CAR',       'AVAILABLE'),
    (6, 'C-03', 'CAR',       'DISABLED');
