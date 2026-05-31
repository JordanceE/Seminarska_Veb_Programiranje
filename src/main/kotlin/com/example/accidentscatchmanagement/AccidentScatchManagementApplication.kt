package com.example.accidentscatchmanagement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class AccidentScatchManagementApplication

fun main(args: Array<String>) {
    runApplication<AccidentScatchManagementApplication>(*args)
}
