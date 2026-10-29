package com.nhom.parking.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank(message = "Tên đăng nhập không được để trống")
		@Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự")
		String username,

		@NotBlank(message = "Mật khẩu không được để trống")
		@Size(min = 8, max = 72, message = "Mật khẩu dài từ 8 đến 72 ký tự")
		String password,

		@NotBlank(message = "Họ tên không được để trống")
		@Size(max = 100, message = "Họ tên tối đa 100 ký tự")
		String fullName) {

}
