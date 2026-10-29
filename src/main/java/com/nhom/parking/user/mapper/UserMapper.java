package com.nhom.parking.user.mapper;

import org.springframework.stereotype.Component;

import com.nhom.parking.user.dto.UserResponse;
import com.nhom.parking.user.entity.User;

@Component
public class UserMapper {

	public UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
				user.getCreatedAt());
	}

}
