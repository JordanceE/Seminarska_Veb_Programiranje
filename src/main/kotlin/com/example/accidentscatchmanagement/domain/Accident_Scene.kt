package com.example.accidentscatchmanagement.domain

import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import jakarta.persistence.*
import com.example.accidentscatchmanagement.domain.commands.*
import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType

import com.example.accidentscatchmanagement.domain.valueobjects.LocationInfo
import com.example.accidentscatchmanagement.domain.valueobjects.MeasurementLine
import com.example.accidentscatchmanagement.domain.valueobjects.VehiclePlacement

import com.example.accidentscatchmanagement.domain.events.*
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.spring.stereotype.Aggregate
import org.axonframework.modelling.command.AggregateLifecycle.apply
import java.time.LocalDateTime

@Aggregate
@Entity
@Table(name = "accident_scene")
class AccidentScene {

    @AggregateIdentifier
    @Id
    lateinit var id: String

    @Enumerated(EnumType.STRING)
    lateinit var roadLayoutType: RoadLayoutType

    @Embedded
    var locationInfo: LocationInfo = LocationInfo()

    @Enumerated(EnumType.STRING)
    lateinit var status: SceneStatus

    var aiConfidence: Double? = null
    var aiSummary: String? = null

    var createdAt: LocalDateTime = LocalDateTime.now()
    var updatedAt: LocalDateTime = LocalDateTime.now()

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "accident_scene_vehicles",
        joinColumns = [JoinColumn(name = "accident_scene_id")]
    )
    var vehicles: MutableList<VehiclePlacement> = mutableListOf()

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "accident_scene_measurements",
        joinColumns = [JoinColumn(name = "accident_scene_id")]
    )
    var measurements: MutableList<MeasurementLine> = mutableListOf()

    constructor()

    @CommandHandler
    constructor(command: CreateAccidentSceneCommand) {
        apply(
            AccidentSceneCreatedEvent(
                accidentSceneId = command.accidentSceneId,
                roadLayoutType = command.roadLayoutType,
                locationInfo = command.locationInfo
            )
        )
    }

    @CommandHandler
    fun handle(command: ChangeRoadLayoutCommand) {
        ensureEditable()
        apply(
            RoadLayoutChangedEvent(
                accidentSceneId = command.accidentSceneId,
                roadLayoutType = command.roadLayoutType
            )
        )
    }

    @CommandHandler
    fun handle(command: UpdateSceneLocationCommand) {
        ensureEditable()
        apply(
            SceneLocationUpdatedEvent(
                accidentSceneId = command.accidentSceneId,
                locationInfo = command.locationInfo
            )
        )
    }
    @CommandHandler
    fun handle(command: StoreFullSceneCommand) {
        ensureEditable()

        command.vehicles.forEach { it.validate() }

        apply(
            FullSceneStoredEvent(
                accidentSceneId = command.accidentSceneId,
                roadLayoutType = command.roadLayoutType,
                locationInfo = command.locationInfo,
                vehicles = command.vehicles,
                measurements = command.measurements
            )
        )
    }

    @EventSourcingHandler
    fun on(event: FullSceneStoredEvent) {
        roadLayoutType = event.roadLayoutType
        locationInfo = event.locationInfo
        vehicles.clear()
        vehicles.addAll(event.vehicles)
        measurements.clear()
        measurements.addAll(event.measurements)
        updatedAt = event.occurredAt
    }

    @CommandHandler
    fun handle(command: AddVehicleCommand) {
        ensureEditable()

        require(command.vehicle.name.isNotBlank()) {
            command.vehicle.validate()
        }

        require(vehicles.none { it.vehicleId == command.vehicle.vehicleId }) {
            "Vehicle with id ${command.vehicle.vehicleId} already exists"
        }

        apply(
            VehicleAddedEvent(
                accidentSceneId = command.accidentSceneId,
                vehicle = command.vehicle
            )
        )
    }

    @CommandHandler
    fun handle(command: UpdateVehicleCommand) {
        ensureEditable()

        require(vehicles.any { it.vehicleId == command.vehicleId }) {
            "Vehicle with id ${command.vehicleId} does not exist"
        }

        apply(
            VehicleUpdatedEvent(
                accidentSceneId = command.accidentSceneId,
                vehicleId = command.vehicleId,
                vehicle = command.vehicle
            )
        )
    }

    @CommandHandler
    fun handle(command: RemoveVehicleCommand) {
        ensureEditable()

        require(vehicles.any { it.vehicleId == command.vehicleId }) {
            "Vehicle with id ${command.vehicleId} does not exist"
        }

        apply(
            VehicleRemovedEvent(
                accidentSceneId = command.accidentSceneId,
                vehicleId = command.vehicleId
            )
        )
    }

    @CommandHandler
    fun handle(command: AddMeasurementCommand) {
        ensureEditable()

        if (command.measurement.fromVehicleId != null) {
            require(vehicles.any { it.vehicleId == command.measurement.fromVehicleId }) {
                "Measurement references a vehicle that does not exist"
            }
        }

        apply(
            MeasurementAddedEvent(
                accidentSceneId = command.accidentSceneId,
                measurement = command.measurement
            )
        )
    }

    @CommandHandler
    fun handle(command: RemoveMeasurementCommand) {
        ensureEditable()

        require(measurements.any { it.measurementId == command.measurementId }) {
            "Measurement with id ${command.measurementId} does not exist"
        }

        apply(
            MeasurementRemovedEvent(
                accidentSceneId = command.accidentSceneId,
                measurementId = command.measurementId
            )
        )
    }

    @CommandHandler
    fun handle(command: StoreAIAnalysisCommand) {
        ensureEditable()

        apply(
            AIAnalysisStoredEvent(
                accidentSceneId = command.accidentSceneId,
                detectedVehicles = command.detectedVehicles,
                confidence = command.confidence,
                summary = command.summary
            )
        )
    }

    @CommandHandler
    fun handle(command: FinalizeAccidentSceneCommand) {
        ensureEditable()

        require(vehicles.isNotEmpty()) {
            "Cannot finalize an accident scene without vehicles"
        }

        apply(
            AccidentSceneFinalizedEvent(
                accidentSceneId = command.accidentSceneId
            )
        )
    }

    @CommandHandler
    fun handle(command: ArchiveAccidentSceneCommand) {
        require(status == SceneStatus.FINALIZED) {
            "Only finalized scenes can be archived"
        }

        apply(
            AccidentSceneArchivedEvent(
                accidentSceneId = command.accidentSceneId
            )
        )
    }

    @EventSourcingHandler
    fun on(event: AccidentSceneCreatedEvent) {
        id = event.accidentSceneId
        roadLayoutType = event.roadLayoutType
        locationInfo = event.locationInfo
        status = SceneStatus.DRAFT
        createdAt = event.occurredAt
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: RoadLayoutChangedEvent) {
        roadLayoutType = event.roadLayoutType
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: SceneLocationUpdatedEvent) {
        locationInfo = event.locationInfo
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: VehicleAddedEvent) {
        vehicles.add(event.vehicle)
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: VehicleUpdatedEvent) {
        vehicles.removeIf { it.vehicleId == event.vehicleId }
        vehicles.add(event.vehicle)
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: VehicleRemovedEvent) {
        vehicles.removeIf { it.vehicleId == event.vehicleId }
        measurements.removeIf { it.fromVehicleId == event.vehicleId }
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: MeasurementAddedEvent) {
        measurements.add(event.measurement)
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: MeasurementRemovedEvent) {
        measurements.removeIf { it.measurementId == event.measurementId }
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: AIAnalysisStoredEvent) {
        vehicles.clear()
        vehicles.addAll(event.detectedVehicles)
        aiConfidence = event.confidence
        aiSummary = event.summary
        status = SceneStatus.AI_ANALYZED
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: AccidentSceneFinalizedEvent) {
        status = SceneStatus.FINALIZED
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: AccidentSceneArchivedEvent) {
        status = SceneStatus.ARCHIVED
        updatedAt = event.occurredAt
    }

    private fun ensureEditable() {
        require(status == SceneStatus.DRAFT || status == SceneStatus.AI_ANALYZED) {
            "Scene cannot be modified when status is $status"
        }
    }
}