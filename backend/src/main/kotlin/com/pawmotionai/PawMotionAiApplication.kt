package com.pawmotionai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PawMotionAiApplication

fun main(args: Array<String>) {
    runApplication<PawMotionAiApplication>(*args)
}
