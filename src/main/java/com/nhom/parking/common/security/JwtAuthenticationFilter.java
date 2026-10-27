package com.nhom.parking.common.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Đọc header "Authorization: Bearer <token>" và đặt AuthenticatedUser vào SecurityContext.
 * Token thiếu hoặc không hợp lệ thì bỏ qua; quy tắc phân quyền trong SecurityConfig quyết định trả 401 hay không.
 * Không đánh dấu @Component để Spring Boot không tự đăng ký thêm một lần như servlet filter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			jwtService.parse(header.substring(BEARER_PREFIX.length()).trim()).ifPresent(user -> {
				var authentication = new UsernamePasswordAuthenticationToken(
						user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			});
		}
		chain.doFilter(request, response);
	}

}
