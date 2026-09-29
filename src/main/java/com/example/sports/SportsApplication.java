package com.example.sports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportsApplication.class, args);
    }
}