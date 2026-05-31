package com.example.accidentscatchmanagement.config
import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.json.JacksonSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class AxonConfiguration {

    @Bean
    @Primary
    fun axonSerializer(objectMapper: ObjectMapper): Serializer {
        return JacksonSerializer.builder()
            .objectMapper(objectMapper)
            .build()
    }
}