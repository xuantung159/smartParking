package com.nhom.parking.common.security;

/**
 * Authenticated user.
 * The JWT filter sets this object as the principal in the SecurityContext,
 * and the controller retrieves it using @AuthenticationPrincipal AuthenticatedUser.
 */
public record AuthenticatedUser(Long id, String username, String role) {
}