package com.kumdoriGrow.backend.api.debug.dto;

public record DbInfoResponse(
    String jdbcUrl,
    String username,
    String database,
    Integer usersCount,
    boolean hasUser999
) {}