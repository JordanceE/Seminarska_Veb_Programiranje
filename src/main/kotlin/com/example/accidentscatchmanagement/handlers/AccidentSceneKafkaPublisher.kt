package com.example.accidentscatchmanagement.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import mk.ukim.finki.accidentscene.domain.events.AccidentSceneFinalizedEvent
import org.axonframework.eventhandling.EventHandler
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component


@Component
@Profile("kafka")
class AccidentSceneKafkaPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    @EventHandler
    fun on(event: AccidentSceneFinalizedEvent) {
        val json = objectMapper.writeValueAsString(event)

        kafkaTemplate.send(
            "accident-scenes-finalized",
            event.accidentSceneId,
            json
        )
    }
}