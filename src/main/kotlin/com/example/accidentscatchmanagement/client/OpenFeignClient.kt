package com.example.accidentscatchmanagement.client

import feign.Logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenFeignConfig {

    @Bean
    fun feignLoggerLevel(): Logger.Level {
        return Logger.Level.BASIC
    }
}