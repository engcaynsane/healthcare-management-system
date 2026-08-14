package com.hms.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, long expiresIn, MeResponse user) {
}