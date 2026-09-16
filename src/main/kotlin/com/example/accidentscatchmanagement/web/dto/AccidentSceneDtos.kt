package com.example.accidentscatchmanagement.web.dto

import com.example.accidentscatchmanagement.domain.Location
import com.example.accidentscatchmanagement.domain.enums.MeasurementType
import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import com.example.accidentscatchmanagement.domain.enums.VehicleType
import com.example.accidentscatchmanagement.domain.valueobjects.LocationInfo
import com.example.accidentscatchmanagement.domain.valueobjects.MeasurementLine
import com.example.accidentscatchmanagement.domain.valueobjects.VehiclePlacement
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class CreateAccidentSceneRequest(
    val roadLayoutType: RoadLayoutType,
    val locationId: String? = null,
    val locationInfo: LocationInput? = null,
    val fileName: String? = null
)
data class LocationInput(
    val name: String,
    val description: String? = null,
    val roadLayoutType: RoadLayoutType = RoadLayoutType.INTERSECTION,
    val backgroundFileName: String? = null,
    val topRoadWidth: Double? = null,
    val topRoadLanes: Int? = null,
    val bottomRoadWidth: Double? = null,
    val bottomRoadLanes: Int? = null,
    val leftRoadWidth: Double? = null,
    val leftRoadLanes: Int? = null,
    val rightRoadWidth: Double? = null,
    val rightRoadLanes: Int? = null,
    val roundaboutDiameter: Double? = null,
    @get:JsonProperty("tJunction")
    val tJunction: Boolean = false,
    // Allows a changed template to retain a previously uploaded picture.
    val photoSourceLocationId: String? = null
)

data class LocationResponse(
    val id: String,
    val name: String,
    val description: String?,
    val roadLayoutType: RoadLayoutType,
    val backgroundFileName: String?,
    val topRoadWidth: Double?,
    val topRoadLanes: Int?,
    val bottomRoadWidth: Double?,
    val bottomRoadLanes: Int?,
    val leftRoadWidth: Double?,
    val leftRoadLanes: Int?,
    val rightRoadWidth: Double?,
    val rightRoadLanes: Int?,
    val roundaboutDiameter: Double?,
    @get:JsonProperty("tJunction") val tJunction: Boolean,
    val photoUrl: String?
)
data class ChangeRoadLayoutRequest(
    val roadLayoutType: RoadLayoutType
)

data class UpdateSceneLocationRequest(
    val locationId: String? = null,
    val locationInfo: LocationInput? = null
)

data class AddVehicleRequest(
    val vehicle: VehiclePlacement
)

data class UpdateVehicleRequest(
    val vehicle: VehiclePlacement
)

data class AddMeasurementRequest(
    val measurement: MeasurementLine
)

data class StoreFullSceneRequest(
    val roadLayoutType: RoadLayoutType,
    val locationId: String? = null,
    val vehicles: List<VehiclePlacement>,
    val measurements: List<MeasurementLine> = emptyList(),
    val locationInfo: LocationInput? = null,
    // Omitted/null preserves the current name; an empty string clears it.
    val fileName: String? = null
)
data class AIAnalysisResponse(
    val location: Map<String, Any?> = emptyMap(),
    val cars: List<DetectedCarDto> = emptyList(),
    val measurements: List<MeasurementLine> = emptyList(),

    @JsonProperty("ai_summary")
    val aiSummary: String? = null,

    val confidence: Double? = null
)

data class DetectedCarDto(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val rotation: Double = 0.0,
    val confidence: Double? = null,
    val vehicleData: DetectedVehicleDataDto = DetectedVehicleDataDto(),
    val note: String? = null
)

data class DetectedVehicleDataDto(
    val name: String = "",
    val model: String? = null,
    val type: String = "sedan",
    val color: String? = null,
    val plate: String? = null,
    val guilty: Boolean = false,
    val comment: String? = null
)
data class AccidentSceneResponse(
    val id: String,
    val roadLayoutType: RoadLayoutType,
    val locationInfo: LocationInfoResponse,
    val status: SceneStatus,
    val aiConfidence: Double?,
    val aiSummary: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val vehicles: List<VehiclePlacementResponse>,
    val measurements: List<MeasurementLineResponse>,
    val locationId: String? = null,
    val location: LocationResponse? = null,
    val fileName: String? = null
)
data class AccidentSceneSummaryResponse(
    val id: String,
    val roadLayoutType: RoadLayoutType,
    val name: String?,
    val status: SceneStatus,
    val aiConfidence: Double?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val locationId: String? = null,
    val fileName: String? = null,
    val locationName: String? = null
)
data class LocationInfoResponse(
    val name: String?,
    val description: String?,
    val topRoadWidth: Double?,
    val topRoadLanes: Int?,
    val bottomRoadWidth: Double?,
    val bottomRoadLanes: Int?,
    val leftRoadWidth: Double?,
    val leftRoadLanes: Int?,
    val rightRoadWidth: Double?,
    val rightRoadLanes: Int?,
    val roundaboutDiameter: Double?,
    @get:JsonProperty("tJunction") val tJunction: Boolean
)

data class CanvasPositionResponse(
    val x: Double,
    val y: Double
)

data class VehiclePlacementResponse(
    val vehicleId: String,
    val name: String,
    val type: VehicleType,
    val model: String?,
    val color: String?,
    val plate: String?,
    val guilty: Boolean,
    val comment: String?,
    val position: CanvasPositionResponse,
    val width: Double,
    val height: Double,
    val rotation: Double,
    val scale: Double,
    val flipped: Boolean,
    val note: String?,
    val confidence: Double?
)

data class MeasurementLineResponse(
    val measurementId: String,
    val fromVehicleId: String?,
    val toVehicleId: String?,
    val type: MeasurementType,
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val lengthMeters: Double,
    val label: String?
)
