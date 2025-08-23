package com.kumdoriGrow.backend.api.debug;

import com.kumdoriGrow.backend.api.debug.dto.DbInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/db-info")
    public DbInfoResponse getDbInfo() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String jdbcUrl = metaData.getURL();
            String username = metaData.getUserName();
            
            // 현재 데이터베이스 조회
            String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            
            // 사용자 수 조회
            Integer usersCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            
            // 사용자 999 존재 여부 조회
            Integer user999Count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, 999);
            boolean hasUser999 = user999Count != null && user999Count > 0;
            
            return new DbInfoResponse(jdbcUrl, username, database, usersCount, hasUser999);
            
        } catch (Exception e) {
            log.error("Failed to retrieve database info", e);
            throw new RuntimeException("Database info retrieval failed", e);
        }
    }
}