package com.kumdoriGrow.backend.api.debug;

import com.kumdoriGrow.backend.config.OcrProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/_debug")
@RequiredArgsConstructor
public class BootDebugController {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final OcrProperties ocrProperties;

    @GetMapping("/boot")
    public Map<String, Object> getBootInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 현재 프로필
            String[] activeProfiles = environment.getActiveProfiles();
            result.put("activeProfiles", activeProfiles.length > 0 ? activeProfiles : new String[]{"default"});
            
            // DB 정보
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String jdbcUrl = metaData.getURL();
                String username = metaData.getUserName();
                
                result.put("jdbcUrl", jdbcUrl);
                result.put("dbUsername", username);
                
                // 현재 데이터베이스 조회
                try {
                    String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
                    result.put("currentDatabase", database != null ? database : "N/A");
                } catch (Exception e) {
                    result.put("currentDatabase", "Query failed: " + e.getMessage());
                }
            }
            
            // OCR 설정 정보 (마스킹)
            Map<String, Object> ocrInfo = new HashMap<>();
            ocrInfo.put("enabled", ocrProperties.enabled());
            ocrInfo.put("apiUrlConfigured", !ocrProperties.apiUrl().isEmpty());
            ocrInfo.put("apiKeyConfigured", !ocrProperties.apiKey().isEmpty());
            ocrInfo.put("apiUrlLength", ocrProperties.apiUrl().length());
            ocrInfo.put("apiKeyLength", ocrProperties.apiKey().length());
            result.put("ocr", ocrInfo);
            
            result.put("status", "SUCCESS");
            
        } catch (Exception e) {
            log.error("Boot debug info retrieval failed", e);
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}