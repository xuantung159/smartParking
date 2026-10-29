package com.nhom.parking.auth.dto;

import com.nhom.parking.user.dto.UserResponse;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
