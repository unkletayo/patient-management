package com.pm.authservice.util;

/** Identity extracted from a successfully validated JWT. */
public record TokenClaims(String email, String role) {
}
