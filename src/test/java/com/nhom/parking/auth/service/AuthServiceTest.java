package com.nhom.parking.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.nhom.parking.auth.dto.LoginRequest;
import com.nhom.parking.auth.dto.LoginResponse;
import com.nhom.parking.auth.dto.RegisterRequest;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.common.security.AuthenticatedUser;
import com.nhom.parking.common.security.JwtService;
import com.nhom.parking.user.dto.UserResponse;
import com.nhom.parking.user.entity.Role;
import com.nhom.parking.user.entity.User;
import com.nhom.parking.user.mapper.UserMapper;
import com.nhom.parking.user.repository.UserRepository;
import com.nhom.parking.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-test-secret-test-secret-123", 3600);
        service = new AuthService(userRepository, new UserMapper(), userService, ENCODER, jwtService);
    }

    @Test
    void loginIssuesTokenCarryingUserIdAndRole() {
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user(2L, "staff", "123456", Role.STAFF)));

        LoginResponse response = service.login(new LoginRequest(" staff ", "123456"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.user().username()).isEqualTo("staff");
        assertThat(jwtService.parse(response.accessToken()))
                .contains(new AuthenticatedUser(2L, "staff", "STAFF"));
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownUserTheSameWay() {
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user(2L, "staff", "123456", Role.STAFF)));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("staff", "wrong")))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThatThrownBy(() -> service.login(new LoginRequest("ghost", "123456")))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void registerAlwaysCreatesCustomerAndLogsIn() {
        when(userService.create("newbie", "password1", "Khách mới", Role.CUSTOMER))
                .thenReturn(new UserResponse(9L, "newbie", "Khách mới", Role.CUSTOMER, null));

        LoginResponse response = service.register(new RegisterRequest("newbie", "password1", "Khách mới"));

        assertThat(jwtService.parse(response.accessToken()))
                .contains(new AuthenticatedUser(9L, "newbie", "CUSTOMER"));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreign = new JwtService("another-secret-another-secret-another-1", 3600)
                .generateToken(new AuthenticatedUser(1L, "admin", "ADMIN"));

        assertThat(jwtService.parse(foreign)).isEmpty();
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
    }

    private static User user(Long id, String username, String password, Role role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setPasswordHash(ENCODER.encode(password));
        user.setFullName(username);
        user.setRole(role);
        return user;
    }

}
