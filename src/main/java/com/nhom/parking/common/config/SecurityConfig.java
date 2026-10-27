package com.nhom.parking.common.config;

import java.io.IOException;
import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom.parking.common.exception.ErrorCode;
import com.nhom.parking.common.security.JwtAuthenticationFilter;
import com.nhom.parking.common.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// @EnableMethodSecurity: bật @PreAuthorize. ADMIN không mặc nhiên có quyền STAFF hay CUSTOMER.
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			ObjectMapper objectMapper) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(
						session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, e) ->
								writeProblem(objectMapper, request, response, ErrorCode.UNAUTHENTICATED))
						.accessDeniedHandler((request, response, e) ->
								writeProblem(objectMapper, request, response, ErrorCode.ACCESS_DENIED)))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/availability").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll());
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private static void writeProblem(ObjectMapper objectMapper, HttpServletRequest request,
			HttpServletResponse response, ErrorCode errorCode) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", errorCode.name());
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), problem);
	}

}
