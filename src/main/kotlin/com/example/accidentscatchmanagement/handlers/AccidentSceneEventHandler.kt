package com.example.accidentscatchmanagement.handlers

import com.example.accidentscatchmanagement.domain.events.*
import com.example.accidentscatchmanagement.domain.events.AccidentSceneCreatedEvent
import com.example.accidentscatchmanagement.domain.events.RoadLayoutChangedEvent
import com.example.accidentscatchmanagement.domain.events.SceneLocationUpdatedEvent
import com.example.accidentscatchmanagement.domain.events.*
import org.axonframework.eventhandling.EventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AccidentSceneEventHandler {

    private val logger = LoggerFactory.getLogger(AccidentSceneEventHandler::class.java)

    @EventHandler
    fun on(event: AccidentSceneCreatedEvent) {
        logger.info(
            "Accident scene created: id={}, roadLayout={}",
            event.accidentSceneId,
            event.roadLayoutType
        )
    }

    @EventHandler
    fun on(event: RoadLayoutChangedEvent) {
        logger.info(
            "Road layout changed for scene {}: {}",
            event.accidentSceneId,
            event.roadLayoutType
        )
    }

    @EventHandler
    fun on(event: SceneLocationUpdatedEvent) {
        logger.info(
            "Scene location updated for scene {}",
            event.accidentSceneId
        )
    }

    @EventHandler
    fun on(event: VehicleAddedEvent) {
        logger.info(
            "Vehicle added to scene {}: vehicleId={}, name={}",
            event.accidentSceneId,
            event.vehicle.vehicleId,
            event.vehicle.name
        )
    }

    @EventHandler
    fun on(event: VehicleUpdatedEvent) {
        logger.info(
            "Vehicle updated in scene {}: vehicleId={}",
            event.accidentSceneId,
            event.vehicleId
        )
    }

    @EventHandler
    fun on(event: VehicleRemovedEvent) {
        logger.info(
            "Vehicle removed from scene {}: vehicleId={}",
            event.accidentSceneId,
            event.vehicleId
        )
    }

    @EventHandler
    fun on(event: MeasurementAddedEvent) {
        logger.info(
            "Measurement added to scene {}: measurementId={}, length={}m",
            event.accidentSceneId,
            event.measurement.measurementId,
            event.measurement.lengthMeters
        )
    }

    @EventHandler
    fun on(event: MeasurementRemovedEvent) {
        logger.info(
            "Measurement removed from scene {}: measurementId={}",
            event.accidentSceneId,
            event.measurementId
        )
    }

    @EventHandler
    fun on(event: AIAnalysisStoredEvent) {
        logger.info(
            "AI analysis stored for scene {}: detectedVehicles={}, confidence={}",
            event.accidentSceneId,
            event.detectedVehicles.size,
            event.confidence
        )
    }

    @EventHandler
    fun on(event: AccidentSceneFinalizedEvent) {
        logger.info(
            "Accident scene finalized: id={}",
            event.accidentSceneId
        )
    }

    @EventHandler
    fun on(event: AccidentSceneArchivedEvent) {
        logger.info(
            "Accident scene archived: id={}",
            event.accidentSceneId
        )
    }
}