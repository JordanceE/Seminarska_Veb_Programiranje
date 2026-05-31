package com.example.accidentscatchmanagement.web.dto

import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.domain.valueobjects.LocationInfo
import com.example.accidentscatchmanagement.domain.valueobjects.MeasurementLine
import com.example.accidentscatchmanagement.domain.valueobjects.VehiclePlacement
import com.fasterxml.jackson.annotation.JsonProperty

data class CreateAccidentSceneRequest(
    val roadLayoutType: RoadLayoutType,
    val locationInfo: LocationInfo = LocationInfo()
)

data class ChangeRoadLayoutRequest(
    val roadLayoutType: RoadLayoutType
)

data class UpdateSceneLocationRequest(
    val locationInfo: LocationInfo
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
    val locationInfo: LocationInfo,
    val vehicles: List<VehiclePlacement>,
    val measurements: List<MeasurementLine>
)
data class AIAnalysisResponse(
    val cars: List<VehiclePlacement> = emptyList(),
    val confidence: Double? = null,

    @JsonProperty("ai_summary")
    val aiSummary: String? = null
)
