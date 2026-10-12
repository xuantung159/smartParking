package com.nhom.parking.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Đổi lỗi thành phản hồi HTTP dạng ProblemDetail, kèm trường "code" là tên ErrorCode.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AppException.class)
	public ProblemDetail handleApp(AppException ex) {
		return toProblem(ex.getErrorCode());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail problem = toProblem(ErrorCode.INVALID_INPUT);
		problem.setProperty("errors", errors);
		return problem;
	}

	private ProblemDetail toProblem(ErrorCode errorCode) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
		problem.setProperty("code", errorCode.name());
		return problem;
	}

}
