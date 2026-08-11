package com.example.accidentscatchmanagement.web

import com.example.accidentscatchmanagement.client.AIAnalysisClient
import com.example.accidentscatchmanagement.domain.commands.StoreAIAnalysisCommand
import com.example.accidentscatchmanagement.domain.enums.VehicleType
import com.example.accidentscatchmanagement.domain.valueobjects.CanvasPosition
import com.example.accidentscatchmanagement.domain.valueobjects.VehiclePlacement
import com.example.accidentscatchmanagement.service.AccidentSceneCommandService
import com.example.accidentscatchmanagement.web.dto.AIAnalysisResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import com.example.accidentscatchmanagement.domain.enums.MeasurementType
import com.example.accidentscatchmanagement.domain.valueobjects.MeasurementLine
import org.springframework.beans.factory.annotation.Value
import kotlin.math.hypot

@RestController
@RequestMapping("/api/accident-scenes")
class AIAnalysisController(
    private val aiAnalysisClient: AIAnalysisClient,
    private val commandService: AccidentSceneCommandService,
    @Value("\${roadwatch.measurements.pixels-per-meter:10.0}")
    private val pixelsPerMeter: Double
) {

    @PostMapping("/{id}/analyze")
    fun analyzeAndStore(
        @PathVariable id: String,
        @RequestPart("image") image: MultipartFile
    ): AIAnalysisResponse {
        val response = aiAnalysisClient.analyzeImage(image)
        val vehicles = response.cars.mapIndexed { index, detected ->
            VehiclePlacement(
                name = detected.vehicleData.name.ifBlank { "V${index + 1}" },
                type = detected.vehicleData.type.toVehicleType(),
                model = detected.vehicleData.model,
                color = detected.vehicleData.color,
                plate = detected.vehicleData.plate,
                guilty = detected.vehicleData.guilty,
                comment = detected.vehicleData.comment,
                position = CanvasPosition(
                    x = detected.x,
                    y = detected.y
                ),
                width = detected.width,
                height = detected.height,
                rotation = detected.rotation,
                scale = 1.0,
                flipped = false,
                note = detected.note,
                confidence = detected.confidence
            )
        }
        require(pixelsPerMeter > 0.0) {
            "roadwatch.measurements.pixels-per-meter must be positive"
        }
        val measurements = vehicles.flatMapIndexed { fromIndex, fromVehicle ->
            vehicles
                .drop(fromIndex + 1)
                .map { toVehicle ->
                    val dx = toVehicle.position.x - fromVehicle.position.x
                    val dy = toVehicle.position.y - fromVehicle.position.y

                    MeasurementLine(
                        fromVehicleId = fromVehicle.vehicleId,
                        toVehicleId = toVehicle.vehicleId,
                        type = MeasurementType.VEHICLE_TO_VEHICLE,
                        x1 = fromVehicle.position.x,
                        y1 = fromVehicle.position.y,
                        x2 = toVehicle.position.x,
                        y2 = toVehicle.position.y,
                        lengthMeters = hypot(dx, dy) / pixelsPerMeter,
                        label = "${fromVehicle.name} ↔ ${toVehicle.name}"
                    )
                }
        }
        commandService.storeAIAnalysis(
            StoreAIAnalysisCommand(
                accidentSceneId = id,
                detectedVehicles = vehicles,
                measurements = measurements,
                confidence = response.confidence,
                summary = response.aiSummary
            )
        )

        return response.copy(
            measurements = measurements
        )
    }
    private fun String.toVehicleType(): VehicleType =
        when (lowercase()) {
            "hatchback" -> VehicleType.HATCHBACK
            "sedan", "car" -> VehicleType.SEDAN
            "coupe" -> VehicleType.COUPE
            "wagon" -> VehicleType.WAGON
            "suv" -> VehicleType.SUV
            "mpv", "minivan" -> VehicleType.MINIVAN
            "pickup" -> VehicleType.PICKUP
            "van" -> VehicleType.VAN
            "truck" -> VehicleType.TRUCK
            "motorcycle" -> VehicleType.MOTORCYCLE
            "bus" -> VehicleType.BUS
            else -> VehicleType.SEDAN
        }
}