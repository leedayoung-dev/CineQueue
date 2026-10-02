
package com.cinequeue.backend.user.dto;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
}