package com.example.accidentscatchmanagement.service

import com.example.accidentscatchmanagement.domain.commands.*

interface AccidentSceneCommandService {
    fun create(command: CreateAccidentSceneCommand): String
    fun changeRoadLayout(command: ChangeRoadLayoutCommand)
    fun updateLocation(command: UpdateSceneLocationCommand)
    fun addVehicle(command: AddVehicleCommand)
    fun updateVehicle(command: UpdateVehicleCommand)
    fun removeVehicle(command: RemoveVehicleCommand)
    fun addMeasurement(command: AddMeasurementCommand)
    fun removeMeasurement(command: RemoveMeasurementCommand)
    fun storeAIAnalysis(command: StoreAIAnalysisCommand)
    fun finalizeScene(command: FinalizeAccidentSceneCommand)
    fun archiveScene(command: ArchiveAccidentSceneCommand)
}