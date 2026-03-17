package com.ops.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // enables @Scheduled TTL eviction in SessionManager
public class OpsChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsChatApplication.class, args);
    }
}
