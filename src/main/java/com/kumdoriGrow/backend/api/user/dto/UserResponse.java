package com.kumdoriGrow.backend.api.user.dto;

import com.kumdoriGrow.backend.domain.user.User;

public record UserResponse(
    Long id,
    String nickname
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNickname());
    }
}