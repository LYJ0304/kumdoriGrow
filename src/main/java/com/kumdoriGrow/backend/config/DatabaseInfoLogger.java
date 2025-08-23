package com.kumdoriGrow.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInfoLogger implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final OcrProperties ocrProperties;

    @Override
    public void run(String... args) throws Exception {
        // 활성 프로필 로깅
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("[STARTUP] Active profiles: {}", activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default");
        
        // DB 정보 로깅
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData.getURL();
            String username = metaData.getUserName();
            
            // 현재 데이터베이스 조회
            String currentDatabase = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            
            log.info("[DB] url={}, user={}, database={}", url, username, currentDatabase);
            
        } catch (Exception e) {
            log.error("[DB] Failed to retrieve database connection info", e);
        }
        
        // OCR 설정 로깅
        log.info("[OCR] enabled={}, apiUrl={}, apiKey={}", 
            ocrProperties.enabled(), 
            ocrProperties.apiUrl().isEmpty() ? "not-configured" : "configured(" + ocrProperties.apiUrl().length() + " chars)",
            ocrProperties.apiKey().isEmpty() ? "not-configured" : "configured(" + ocrProperties.apiKey().length() + " chars)");
    }
}