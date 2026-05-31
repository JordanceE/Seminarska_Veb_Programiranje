package com.example.accidentscatchmanagement.web

import com.example.accidentscatchmanagement.domain.commands.ArchiveAccidentSceneCommand
import com.example.accidentscatchmanagement.domain.commands.FinalizeAccidentSceneCommand
import com.example.accidentscatchmanagement.domain.commands.*
import com.example.accidentscatchmanagement.domain.ids.AccidentSceneId
import com.example.accidentscatchmanagement.service.AccidentSceneCommandService
import com.example.accidentscatchmanagement.web.dto.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/accident-scenes")
class AccidentSceneCommandController(
    private val commandService: AccidentSceneCommandService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateAccidentSceneRequest): Map<String, String> {
        val id = AccidentSceneId().value

        commandService.create(
            CreateAccidentSceneCommand(
                accidentSceneId = id,
                roadLayoutType = request.roadLayoutType,
                locationInfo = request.locationInfo
            )
        )

        return mapOf("id" to id)
    }
    @PutMapping("/{id}/full-scene")
    fun storeFullScene(
        @PathVariable id: String,
        @RequestBody request: StoreFullSceneRequest
    ) {
        commandService.storeFullScene(
            StoreFullSceneCommand(
                accidentSceneId = id,
                roadLayoutType = request.roadLayoutType,
                locationInfo = request.locationInfo,
                vehicles = request.vehicles,
                measurements = request.measurements
            )
        )
    }
    @PatchMapping("/{id}/road-layout")
    fun changeRoadLayout(
        @PathVariable id: String,
        @RequestBody request: ChangeRoadLayoutRequest
    ) {
        commandService.changeRoadLayout(
            ChangeRoadLayoutCommand(
                accidentSceneId = id,
                roadLayoutType = request.roadLayoutType
            )
        )
    }

    @PatchMapping("/{id}/location")
    fun updateLocation(
        @PathVariable id: String,
        @RequestBody request: UpdateSceneLocationRequest
    ) {
        commandService.updateLocation(
            UpdateSceneLocationCommand(
                accidentSceneId = id,
                locationInfo = request.locationInfo
            )
        )
    }

    @PostMapping("/{id}/vehicles")
    fun addVehicle(
        @PathVariable id: String,
        @RequestBody request: AddVehicleRequest
    ) {
        commandService.addVehicle(
            AddVehicleCommand(
                accidentSceneId = id,
                vehicle = request.vehicle
            )
        )
    }

    @PutMapping("/{id}/vehicles/{vehicleId}")
    fun updateVehicle(
        @PathVariable id: String,
        @PathVariable vehicleId: String,
        @RequestBody request: UpdateVehicleRequest
    ) {
        commandService.updateVehicle(
            UpdateVehicleCommand(
                accidentSceneId = id,
                vehicleId = vehicleId,
                vehicle = request.vehicle.copy(vehicleId = vehicleId)
            )
        )
    }

    @DeleteMapping("/{id}/vehicles/{vehicleId}")
    fun removeVehicle(
        @PathVariable id: String,
        @PathVariable vehicleId: String
    ) {
        commandService.removeVehicle(
            RemoveVehicleCommand(
                accidentSceneId = id,
                vehicleId = vehicleId
            )
        )
    }

    @PostMapping("/{id}/measurements")
    fun addMeasurement(
        @PathVariable id: String,
        @RequestBody request: AddMeasurementRequest
    ) {
        commandService.addMeasurement(
            AddMeasurementCommand(
                accidentSceneId = id,
                measurement = request.measurement
            )
        )
    }

    @DeleteMapping("/{id}/measurements/{measurementId}")
    fun removeMeasurement(
        @PathVariable id: String,
        @PathVariable measurementId: String
    ) {
        commandService.removeMeasurement(
            RemoveMeasurementCommand(
                accidentSceneId = id,
                measurementId = measurementId
            )
        )
    }

    @PostMapping("/{id}/finalize")
    fun finalizeScene(@PathVariable id: String) {
        commandService.finalizeScene(
            FinalizeAccidentSceneCommand(
                accidentSceneId = id
            )
        )
    }

    @PostMapping("/{id}/archive")
    fun archiveScene(@PathVariable id: String) {
        commandService.archiveScene(
            ArchiveAccidentSceneCommand(
                accidentSceneId = id
            )
        )
    }
}