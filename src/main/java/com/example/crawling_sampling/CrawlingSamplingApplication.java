package com.example.crawling_sampling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrawlingSamplingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrawlingSamplingApplication.class, args);
    }

}
