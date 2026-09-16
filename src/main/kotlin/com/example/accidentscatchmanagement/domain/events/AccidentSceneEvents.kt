package com.example.accidentscatchmanagement.domain.events

import com.example.accidentscatchmanagement.domain.Location
import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.domain.ids.LocationId
import com.example.accidentscatchmanagement.domain.valueobjects.LocationInfo
import com.example.accidentscatchmanagement.domain.valueobjects.MeasurementLine
import com.example.accidentscatchmanagement.domain.valueobjects.VehiclePlacement
import java.time.LocalDateTime

data class AccidentSceneCreatedEvent(
    val accidentSceneId: String,
    val roadLayoutType: RoadLayoutType,
    val locationId: String? = null,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
    val fileName: String? = null
)

data class RoadLayoutChangedEvent(
    val accidentSceneId: String,
    val roadLayoutType: RoadLayoutType,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

data class SceneLocationUpdatedEvent(
    val accidentSceneId: String,
    val locationId: String? = null,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
    val roadLayoutType: RoadLayoutType? = null
)

data class VehicleAddedEvent(
    val accidentSceneId: String,
    val vehicle: VehiclePlacement,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

data class VehicleUpdatedEvent(
    val accidentSceneId: String,
    val vehicleId: String,
    val vehicle: VehiclePlacement,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

data class VehicleRemovedEvent(
    val accidentSceneId: String,
    val vehicleId: String,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

data class MeasurementAddedEvent(
    val accidentSceneId: String,
    val measurement: MeasurementLine,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

data class MeasurementRemovedEvent(
    val accidentSceneId: String,
    val measurementId: String,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

data class AIAnalysisStoredEvent(
    val accidentSceneId: String,
    val detectedVehicles: List<VehiclePlacement>,
    val measurements: List<MeasurementLine> = emptyList(),
    val confidence: Double?,
    val summary: String?,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)
data class FullSceneStoredEvent(
    val accidentSceneId: String,
    val roadLayoutType: RoadLayoutType,
    val locationId: String? = null,
    val vehicles: List<VehiclePlacement>,
    val measurements: List<MeasurementLine> = emptyList(),
    val occurredAt: LocalDateTime = LocalDateTime.now(),
    // Null in historical events means the name was not part of that update.
    val fileName: String? = null
)
data class AccidentSceneFinalizedEvent(
    val accidentSceneId: String,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

data class AccidentSceneArchivedEvent(
    val accidentSceneId: String,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)
