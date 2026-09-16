package com.example.accidentscatchmanagement.domain

import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import jakarta.persistence.*
import com.example.accidentscatchmanagement.domain.commands.*
import com.example.accidentscatchmanagement.domain.enums.MeasurementType
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

@Aggregate(repository = "axonAccidentSceneRepository")
@Entity
@Table(name = "accident_scene", indexes = [Index(name = "idx_scene_location", columnList = "location_id")])
class AccidentScene {

    @AggregateIdentifier
    @Id
    lateinit var id: String

    @Enumerated(EnumType.STRING)
    lateinit var roadLayoutType: RoadLayoutType

    @Column(name = "file_name", length = 255)
    var fileName: String? = null

    @Column(name = "location_id")
    var locationId: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "location_id",
        insertable = false,
        updatable = false
    )
    var location: Location? = null

    @Enumerated(EnumType.STRING)
    lateinit var status: SceneStatus

    var aiConfidence: Double? = null
    var aiSummary: String? = null

    var createdAt: LocalDateTime = LocalDateTime.now()
    var updatedAt: LocalDateTime = LocalDateTime.now()

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "accident_scene_vehicles",
        joinColumns = [JoinColumn(name = "accident_scene_id")]
    )
    @OrderColumn(name = "vehicle_order")
    var vehicles: MutableList<VehiclePlacement> = mutableListOf()

    @ElementCollection(fetch = FetchType.LAZY)
    @OrderColumn(name = "measurement_order")
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
                locationId = command.locationId,
                fileName = normalizedFileName(command.fileName),
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
                locationId = command.locationId,
                roadLayoutType = command.roadLayoutType
            )
        )
    }
    @CommandHandler
    fun handle(command: StoreFullSceneCommand) {
        ensureEditable()

        command.vehicles.forEach { it.validate() }
        val vehicleIds =
            command.vehicles
                .map { it.vehicleId }
                .toSet()
        require(vehicleIds.size == command.vehicles.size) {
            "Vehicle IDs must be unique"
        }
        val measurementIds =
            command.measurements
                .map { it.measurementId }
        require(measurementIds.distinct().size == measurementIds.size) {
            "Measurement IDs must be unique"
        }
        command.measurements.forEach { measurement ->
            require(measurement.x1.isFinite()) {
                "Measurement x1 must be finite"
            }

            require(measurement.y1.isFinite()) {
                "Measurement y1 must be finite"
            }

            require(measurement.x2.isFinite()) {
                "Measurement x2 must be finite"
            }

            require(measurement.y2.isFinite()) {
                "Measurement y2 must be finite"
            }

            require(
                measurement.lengthMeters.isFinite() &&
                        measurement.lengthMeters >= 0.0
            ) {
                "Measurement length must be a finite non-negative number"
            }

            measurement.fromVehicleId?.let {
                require(it in vehicleIds) {
                    "Measurement references missing source vehicle $it"
                }
            }

            measurement.toVehicleId?.let {
                require(it in vehicleIds) {
                    "Measurement references missing target vehicle $it"
                }
            }

            if (
                measurement.type ==
                MeasurementType.VEHICLE_TO_VEHICLE
            ) {
                require(measurement.fromVehicleId != null) {
                    "Vehicle-to-vehicle measurement requires a source vehicle"
                }

                require(measurement.toVehicleId != null) {
                    "Vehicle-to-vehicle measurement requires a target vehicle"
                }

                require(
                    measurement.fromVehicleId !=
                            measurement.toVehicleId
                ) {
                    "A vehicle cannot be measured to itself"
                }
            }
        }
        apply(
            FullSceneStoredEvent(
                accidentSceneId = command.accidentSceneId,
                roadLayoutType = command.roadLayoutType,
                locationId = command.locationId,
                vehicles = command.vehicles,
                measurements = command.measurements,
                fileName = normalizedFileName(command.fileName ?: fileName) ?: ""
            )
        )
    }

    @EventSourcingHandler
    fun on(event: FullSceneStoredEvent) {
        event.fileName?.let { fileName = normalizedFileName(it) }
        roadLayoutType = event.roadLayoutType
        locationId = event.locationId
        vehicles.clear()
        vehicles.addAll(event.vehicles)
        measurements.clear()
        measurements.addAll(event.measurements)
        updatedAt = event.occurredAt
    }

    @CommandHandler
    fun handle(command: AddVehicleCommand) {
        ensureEditable()


        command.vehicle.validate()


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
        command.vehicle.validate()
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
        require(
            measurements.none {
                it.measurementId ==
                        command.measurement.measurementId
            }
        ) {
            "Measurement with id ${command.measurement.measurementId} already exists"
        }

        command.measurement.fromVehicleId?.let { vehicleId ->
            require(vehicles.any { it.vehicleId == vehicleId }) {
                "Measurement references a source vehicle that does not exist"
            }
        }

        command.measurement.toVehicleId?.let { vehicleId ->
            require(vehicles.any { it.vehicleId == vehicleId }) {
                "Measurement references a target vehicle that does not exist"
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
                measurements = command.measurements,
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
        require(locationId != null) {
            "Cannot finalize an accident scene without a location"
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
        fileName = event.fileName
        roadLayoutType = event.roadLayoutType
        locationId = event.locationId
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
        locationId = event.locationId
        event.roadLayoutType?.let { roadLayoutType = it }
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: VehicleAddedEvent) {
        vehicles.add(event.vehicle)
        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: VehicleUpdatedEvent) {
        val vehicleIndex =
            vehicles.indexOfFirst {
                it.vehicleId == event.vehicleId
            }

        if (vehicleIndex >= 0) {
            vehicles[vehicleIndex] =
                event.vehicle
        }

        updatedAt = event.occurredAt
    }

    @EventSourcingHandler
    fun on(event: VehicleRemovedEvent) {
        vehicles.removeIf {
            it.vehicleId == event.vehicleId
        }

        measurements.removeIf {
            it.fromVehicleId == event.vehicleId ||
                    it.toVehicleId == event.vehicleId
        }

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

        /*
         * AI analysis replaces vehicle-linked measurements because
         * the detected vehicles receive new IDs. Pure point-to-point
         * measurements are retained.
         */
        measurements.removeIf {
            it.fromVehicleId != null ||
                    it.toVehicleId != null
        }

        measurements.addAll(event.measurements)

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

    private fun normalizedFileName(value: String?): String? {
        val name = value?.trim()?.takeIf { it.isNotEmpty() }
        require(name == null || name.length <= 255) { "Accident file name must be at most 255 characters" }
        return name
    }

    private fun ensureEditable() {
        require(status == SceneStatus.DRAFT || status == SceneStatus.AI_ANALYZED) {
            "Scene cannot be modified when status is $status"
        }
    }
}
