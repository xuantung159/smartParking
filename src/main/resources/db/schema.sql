-- Cấu trúc bảng. Chạy mỗi lần ứng dụng khởi động, nên mọi lệnh phải chạy lại được (IF NOT EXISTS).
-- Hibernate chỉ đối chiếu (ddl-auto: validate): sửa entity thì phải sửa bảng ở đây.

-- A: tài khoản
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL, -- ADMIN | STAFF | CUSTOMER
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);

-- A: chỗ đỗ
CREATE TABLE IF NOT EXISTS parking_slots (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    code         VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL, -- MOTORBIKE | CAR
    status       VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE | OCCUPIED | DISABLED
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_slots_code UNIQUE (code)
);

-- B: đặt chỗ trước
CREATE TABLE IF NOT EXISTS reservations (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    code          VARCHAR(20) NOT NULL, -- mã đặt chỗ khách đưa cho nhân viên khi vào bãi
    user_id       BIGINT      NOT NULL,
    slot_id       BIGINT      NOT NULL,
    license_plate VARCHAR(20) NOT NULL,
    start_time    DATETIME(6) NOT NULL,
    end_time      DATETIME(6) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | CANCELLED | USED
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_reservations_code UNIQUE (code),
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reservations_slot FOREIGN KEY (slot_id) REFERENCES parking_slots (id),
    CONSTRAINT ck_reservations_time CHECK (end_time > start_time),
    INDEX idx_reservations_slot_time (slot_id, start_time, end_time),
    INDEX idx_reservations_user (user_id)
);

-- C: lượt gửi xe
CREATE TABLE IF NOT EXISTS parking_sessions (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    slot_id        BIGINT      NOT NULL,
    reservation_id BIGINT      NULL, -- NULL nếu khách vãng lai
    license_plate  VARCHAR(20) NOT NULL,
    vehicle_type   VARCHAR(20) NOT NULL, -- MOTORBIKE | CAR
    check_in_time  DATETIME(6) NOT NULL,
    check_out_time DATETIME(6) NULL,
    fee            BIGINT      NULL, -- đồng, có khi xe ra
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | COMPLETED | CANCELLED
    PRIMARY KEY (id),
    CONSTRAINT fk_sessions_slot FOREIGN KEY (slot_id) REFERENCES parking_slots (id),
    CONSTRAINT fk_sessions_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    INDEX idx_sessions_plate_status (license_plate, status),
    INDEX idx_sessions_status (status)
);
