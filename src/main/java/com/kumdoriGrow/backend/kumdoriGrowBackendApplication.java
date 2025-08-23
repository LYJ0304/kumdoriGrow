package com.kumdoriGrow.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.kumdoriGrow.backend")
public class kumdoriGrowBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(kumdoriGrowBackendApplication.class, args);
	}

}
