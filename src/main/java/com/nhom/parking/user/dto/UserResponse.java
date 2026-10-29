package com.nhom.parking.user.dto;

import java.time.LocalDateTime;

import com.nhom.parking.user.entity.Role;

public record UserResponse(Long id, String username, String fullName, Role role, LocalDateTime createdAt) {
}
