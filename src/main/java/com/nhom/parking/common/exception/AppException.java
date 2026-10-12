package com.nhom.parking.common.exception;

/**
 * Lỗi duy nhất mà service ném ra: throw new AppException(ErrorCode.SLOT_NOT_FOUND).
 */
public class AppException extends RuntimeException {

	private final ErrorCode errorCode;

	public AppException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

}
