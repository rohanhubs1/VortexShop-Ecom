package com.ecom.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
public class EcomPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcomPlatformApplication.class, args);
    }
}
