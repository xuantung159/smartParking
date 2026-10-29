package com.nhom.parking.common.security;

/**
 * Authenticated user.
 * The JWT filter sets this object as the principal in the SecurityContext,
 * and the controller retrieves it using @AuthenticationPrincipal AuthenticatedUser.
 */
public record AuthenticatedUser(Long id, String username, String role) {

	/** STAFF và ADMIN được thao tác trên dữ liệu của người khác (vd. lịch đặt của khách). */
	public boolean isStaffOrAdmin() {
		return "STAFF".equals(role) || "ADMIN".equals(role);
	}

}