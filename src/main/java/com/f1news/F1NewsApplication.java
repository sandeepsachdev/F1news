package com.f1news;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class F1NewsApplication {
    public static void main(String[] args) {
        SpringApplication.run(F1NewsApplication.class, args);
    }
}
