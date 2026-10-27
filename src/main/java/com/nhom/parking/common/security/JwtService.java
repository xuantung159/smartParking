package com.nhom.parking.common.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Cấp và kiểm tra access token JWT (HS256). Subject là id người dùng, kèm claim username và role.
 */
@Service
public class JwtService {

	private static final Logger log = LoggerFactory.getLogger(JwtService.class);

	private final SecretKey key;
	private final long expiresInSeconds;

	public JwtService(@Value("${app.jwt.secret:}") String secret,
			@Value("${app.jwt.expires-in:3600}") long expiresInSeconds) {
		if (secret == null || secret.isBlank()) {
			// Không cấu hình JWT_SECRET: sinh khoá ngẫu nhiên, token mất hiệu lực khi khởi động lại.
			byte[] random = new byte[32];
			new SecureRandom().nextBytes(random);
			this.key = Keys.hmacShaKeyFor(random);
			log.warn("JWT_SECRET chưa được cấu hình, dùng khoá ngẫu nhiên cho lần chạy này");
		} else {
			// Khoá HS256 phải dài ít nhất 32 byte, nếu ngắn hơn sẽ lỗi ngay khi khởi động.
			this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		}
		this.expiresInSeconds = expiresInSeconds;
	}

	public String generateToken(AuthenticatedUser user) {
		Date now = new Date();
		return Jwts.builder()
				.subject(String.valueOf(user.id()))
				.claim("username", user.username())
				.claim("role", user.role())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expiresInSeconds * 1000))
				.signWith(key)
				.compact();
	}

	/** Trả về người dùng trong token; rỗng nếu token sai chữ ký, hết hạn hoặc sai định dạng. */
	public Optional<AuthenticatedUser> parse(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			return Optional.of(new AuthenticatedUser(
					Long.valueOf(claims.getSubject()),
					claims.get("username", String.class),
					claims.get("role", String.class)));
		} catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	public long getExpiresInSeconds() {
		return expiresInSeconds;
	}

}
