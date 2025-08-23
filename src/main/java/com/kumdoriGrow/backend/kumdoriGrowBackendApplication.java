package com.kumdoriGrow.backend;

import com.kumdoriGrow.backend.config.OcrProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.kumdoriGrow.backend")
@EnableConfigurationProperties(OcrProperties.class)
public class kumdoriGrowBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(kumdoriGrowBackendApplication.class, args);
	}

}
