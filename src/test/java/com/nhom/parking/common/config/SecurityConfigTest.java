package com.nhom.parking.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.nhom.parking.auth.service.AuthService;
import com.nhom.parking.availability.service.AvailabilityService;
import com.nhom.parking.common.security.AuthenticatedUser;
import com.nhom.parking.common.security.JwtService;
import com.nhom.parking.reservation.service.ReservationService;
import com.nhom.parking.session.service.ParkingSessionService;
import com.nhom.parking.slot.service.SlotService;
import com.nhom.parking.user.service.UserService;

/**
 * Kiểm tra phân quyền (@PreAuthorize trên controller): chỉ tầng web/security, service đều là mock.
 * Mỗi dòng: method, path, vai trò được phép; các vai trò khác phải nhận 403, không token nhận 401.
 */
@WebMvcTest
@Import({ SecurityConfig.class, JwtService.class })
@TestPropertySource(properties = "app.jwt.secret=test-secret-test-secret-test-secret-123")
class SecurityConfigTest {

	private static final String[] ROLES = { "ADMIN", "STAFF", "CUSTOMER" };

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private RequestMappingHandlerMapping handlerMapping;

	@MockitoBean
	private AuthService authService;
	@MockitoBean
	private UserService userService;
	@MockitoBean
	private SlotService slotService;
	@MockitoBean
	private AvailabilityService availabilityService;
	@MockitoBean
	private ReservationService reservationService;
	@MockitoBean
	private ParkingSessionService parkingSessionService;

	@ParameterizedTest(name = "{0} {1} chỉ cho {2}")
	@CsvSource({
			"POST,   /api/users,                  ADMIN",
			"GET,    /api/users,                  ADMIN",
			"POST,   /api/slots,                  ADMIN",
			"DELETE, /api/slots/1,                ADMIN",
			"POST,   /api/reservations,           CUSTOMER",
			"GET,    /api/reservations/me,        CUSTOMER",
			"GET,    /api/reservations/1,         CUSTOMER",
			"DELETE, /api/reservations/1,         CUSTOMER",
			"POST,   /api/sessions/check-in,      STAFF",
			"POST,   /api/sessions/1/check-out,   STAFF",
			"GET,    /api/sessions,               STAFF",
			"GET,    /api/sessions/1,             STAFF",
			"DELETE, /api/sessions/1,             STAFF",
	})
	void protectedEndpointRequiresExactRole(String method, String path, String allowedRole) throws Exception {
		mvc.perform(call(method, path))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		for (String role : ROLES) {
			if (!role.equals(allowedRole)) {
				mvc.perform(call(method, path).header(HttpHeaders.AUTHORIZATION, bearer(role)))
						.andExpect(status().isForbidden())
						.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
			}
		}
	}

	@Test
	void allowedRolePassesSecurity() throws Exception {
		mvc.perform(call("GET", "/api/users").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
				.andExpect(status().isOk());
		mvc.perform(call("DELETE", "/api/slots/1").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
				.andExpect(status().isNoContent());
		mvc.perform(call("GET", "/api/reservations/me").header(HttpHeaders.AUTHORIZATION, bearer("CUSTOMER")))
				.andExpect(status().isOk());
		mvc.perform(call("GET", "/api/sessions").header(HttpHeaders.AUTHORIZATION, bearer("STAFF")))
				.andExpect(status().isOk());
	}

	@Test
	void publicEndpointsNeedNoToken() throws Exception {
		mvc.perform(call("POST", "/api/auth/login").content("{\"username\":\"a\",\"password\":\"b\"}"))
				.andExpect(status().isOk());
		mvc.perform(call("POST", "/api/auth/register")
				.content("{\"username\":\"a\",\"password\":\"password1\",\"fullName\":\"A\"}"))
				.andExpect(status().isCreated());
		mvc.perform(call("GET", "/api/availability?vehicleType=CAR"
				+ "&startTime=2030-01-01T08:00:00&endTime=2030-01-01T09:00:00"))
				.andExpect(status().isOk());
	}

	@Test
	void meNeedsAnyLogin() throws Exception {
		mvc.perform(call("GET", "/api/auth/me")).andExpect(status().isUnauthorized());
		for (String role : ROLES) {
			mvc.perform(call("GET", "/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(role)))
					.andExpect(status().isOk());
		}
	}

	@Test
	void invalidTokenIsTreatedAsAnonymous() throws Exception {
		mvc.perform(call("GET", "/api/sessions").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());
	}

	/** Endpoint /api nào cũng phải khai báo quyền (@PreAuthorize trên method hoặc class), kể cả endpoint công khai. */
	@Test
	void everyApiEndpointDeclaresItsPermission() {
		List<String> missing = handlerMapping.getHandlerMethods().entrySet().stream()
				.filter(e -> e.getKey().getPatternValues().stream().anyMatch(p -> p.startsWith("/api")))
				.filter(e -> !AnnotatedElementUtils.hasAnnotation(e.getValue().getMethod(), PreAuthorize.class)
						&& !AnnotatedElementUtils.hasAnnotation(e.getValue().getBeanType(), PreAuthorize.class))
				.map(e -> e.getKey().toString())
				.toList();

		assertThat(handlerMapping.getHandlerMethods()).isNotEmpty();
		assertThat(missing).as("endpoint thiếu @PreAuthorize").isEmpty();
	}

	// Body hợp lệ cho các endpoint POST: @PreAuthorize chạy sau khi body được đọc và validate,
	// nên body sai sẽ ra 400 trước khi xét vai trò.
	private static final Map<String, String> VALID_BODIES = Map.of(
			"/api/users", "{\"username\":\"u\",\"password\":\"password1\",\"fullName\":\"U\",\"role\":\"STAFF\"}",
			"/api/slots", "{\"code\":\"A-01\",\"vehicleType\":\"CAR\"}",
			"/api/reservations", "{\"slotId\":1,\"licensePlate\":\"59A-1\","
					+ "\"startTime\":\"2099-01-01T08:00:00\",\"endTime\":\"2099-01-01T09:00:00\"}",
			"/api/sessions/check-in", "{\"licensePlate\":\"59A-1\",\"vehicleType\":\"CAR\"}");

	private static MockHttpServletRequestBuilder call(String method, String path) {
		MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path)
				.contentType(MediaType.APPLICATION_JSON);
		String body = "POST".equals(method) ? VALID_BODIES.get(path) : null;
		return body == null ? builder : builder.content(body);
	}

	private String bearer(String role) {
		return "Bearer " + jwtService.generateToken(new AuthenticatedUser(1L, role.toLowerCase(), role));
	}

}
