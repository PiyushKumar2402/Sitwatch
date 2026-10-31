package com.sitewatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SiteWatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiteWatchApplication.class, args);
    }
}
