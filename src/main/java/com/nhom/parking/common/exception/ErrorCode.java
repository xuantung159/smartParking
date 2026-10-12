package com.nhom.parking.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	// chung
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc mã thông báo không hợp lệ"),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "Không có quyền truy cập"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống"),

	// auth, user
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"),
	USERNAME_EXISTED(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại"),

	// slot
	SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy chỗ đỗ"),
	SLOT_CODE_EXISTED(HttpStatus.CONFLICT, "Mã chỗ đỗ đã tồn tại"),
	SLOT_UNAVAILABLE(HttpStatus.CONFLICT, "Không còn chỗ đỗ phù hợp"),

	// reservation
	RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đặt chỗ"),
	RESERVATION_TIME_CONFLICT(HttpStatus.CONFLICT, "Chỗ đỗ đã được đặt trong khung giờ này"),
	RESERVATION_NOT_ACTIVE(HttpStatus.CONFLICT, "Đặt chỗ đã huỷ hoặc đã sử dụng"),

	// session
	SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy lượt gửi xe"),
	VEHICLE_ALREADY_PARKED(HttpStatus.CONFLICT, "Xe đang gửi trong bãi"),
	SESSION_ALREADY_CLOSED(HttpStatus.CONFLICT, "Lượt gửi xe đã kết thúc");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

}
