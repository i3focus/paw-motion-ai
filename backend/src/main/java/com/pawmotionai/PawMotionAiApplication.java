package com.pawmotionai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PawMotionAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PawMotionAiApplication.class, args);
    }
}
