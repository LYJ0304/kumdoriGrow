package com.kumdoriGrow.backend.api.user.dto;

import com.kumdoriGrow.backend.domain.user.User;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String nickname,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNickname(), user.getCreatedAt());
    }
}