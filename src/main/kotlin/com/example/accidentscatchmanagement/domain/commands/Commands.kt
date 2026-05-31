package com.example.accidentscatchmanagement.domain.commands

import jakarta.persistence.Embeddable

import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.domain.valueobjects.LocationInfo
import com.example.accidentscatchmanagement.domain.valueobjects.MeasurementLine
import com.example.accidentscatchmanagement.domain.valueobjects.VehiclePlacement
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class CreateAccidentSceneCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val roadLayoutType: RoadLayoutType,
    val locationInfo: LocationInfo
)

data class ChangeRoadLayoutCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val roadLayoutType: RoadLayoutType
)
data class StoreFullSceneCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val roadLayoutType: RoadLayoutType,
    val locationInfo: LocationInfo,
    val vehicles: List<VehiclePlacement>,
    val measurements: List<MeasurementLine>
)
data class UpdateSceneLocationCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val locationInfo: LocationInfo
)

data class AddVehicleCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val vehicle: VehiclePlacement
)

data class UpdateVehicleCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val vehicleId: String,
    val vehicle: VehiclePlacement
)

data class RemoveVehicleCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val vehicleId: String
)

data class AddMeasurementCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val measurement: MeasurementLine
)

data class RemoveMeasurementCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val measurementId: String
)

data class StoreAIAnalysisCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String,
    val detectedVehicles: List<VehiclePlacement>,
    val confidence: Double?,
    val summary: String?
)

data class FinalizeAccidentSceneCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String
)

data class ArchiveAccidentSceneCommand(
    @TargetAggregateIdentifier
    val accidentSceneId: String
)