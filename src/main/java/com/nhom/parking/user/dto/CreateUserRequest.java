package com.nhom.parking.user.dto;

import com.nhom.parking.user.entity.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

		@NotBlank(message = "Tên đăng nhập không được để trống")
		@Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự")
		String username,

		// BCrypt chỉ dùng 72 byte đầu của mật khẩu.
		@NotBlank(message = "Mật khẩu không được để trống")
		@Size(min = 8, max = 72, message = "Mật khẩu dài từ 8 đến 72 ký tự")
		String password,

		@NotBlank(message = "Họ tên không được để trống")
		@Size(max = 100, message = "Họ tên tối đa 100 ký tự")
		String fullName,

		@NotNull(message = "Vai trò không được để trống")
		Role role) {

}
