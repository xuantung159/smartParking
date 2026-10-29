package com.nhom.parking.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nhom.parking.common.dto.PageResponse;
import com.nhom.parking.common.exception.AppException;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.user.dto.CreateUserRequest;
import com.nhom.parking.user.dto.UserResponse;
import com.nhom.parking.user.entity.Role;
import com.nhom.parking.user.entity.User;
import com.nhom.parking.user.mapper.UserMapper;
import com.nhom.parking.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, new UserMapper(), ENCODER, entityManager);
    }

    @Test
    void createHashesPasswordAndTrimsFields() {
        when(userRepository.existsByUsername("staff2")).thenReturn(false);

        UserResponse response = service.create(new CreateUserRequest(" staff2 ", "password1", " Nhân viên 2 ", Role.STAFF));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("staff2");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("password1");
        assertThat(ENCODER.matches("password1", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.fullName()).isEqualTo("Nhân viên 2");
        assertThat(response.role()).isEqualTo(Role.STAFF);
    }

    @Test
    void createRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserRequest("admin", "password1", "A", Role.ADMIN)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USERNAME_EXISTED);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void listFiltersByRoleAndReturnsPageEnvelope() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id"));
        User customer = new User();
        customer.setUsername("customer");
        customer.setRole(Role.CUSTOMER);
        when(userRepository.findByRole(Role.CUSTOMER, pageable)).thenReturn(new PageImpl<>(List.of(customer), pageable, 5));

        PageResponse<UserResponse> page = service.list(Role.CUSTOMER, 0, 2);

        assertThat(page.items()).extracting(UserResponse::username).containsExactly("customer");
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(5);
    }

    @Test
    void listRejectsInvalidPaging() {
        assertThatThrownBy(() -> service.list(null, -1, 20)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.list(null, 0, 0)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.list(null, 0, 101)).isInstanceOf(AppException.class);
    }

}
