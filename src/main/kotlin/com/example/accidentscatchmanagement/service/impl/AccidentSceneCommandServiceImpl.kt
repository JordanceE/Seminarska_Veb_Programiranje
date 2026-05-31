package com.example.accidentscatchmanagement.service.impl

import com.example.accidentscatchmanagement.domain.commands.*
import com.example.accidentscatchmanagement.service.AccidentSceneCommandService
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class AccidentSceneCommandServiceImpl(
    private val commandGateway: CommandGateway
) : AccidentSceneCommandService {

    override fun create(command: CreateAccidentSceneCommand): String {
        commandGateway.sendAndWait<Any>(command)
        return command.accidentSceneId
    }

    override fun changeRoadLayout(command: ChangeRoadLayoutCommand) {
        commandGateway.sendAndWait<Any>(command)
    }

    override fun updateLocation(command: UpdateSceneLocationCommand) {
        commandGateway.sendAndWait<Any>(command)
    }

    override fun addVehicle(command: AddVehicleCommand) {
        commandGateway.sendAndWait<Any>(command)
    }

    override fun updateVehicle(command: UpdateVehicleCommand) {
        commandGateway.sendAndWait<Any>(command)
    }

    override fun removeVehicle(command: RemoveVehicleCommand) {
        commandGateway.sendAndWait<Any>(command)
    }

    override fun addMeasurement(command: AddMeasurementCommand) {
        commandGateway.sendAndWait<Any>(command)
    }

    override fun removeMeasurement(command: RemoveMeasurementCommand) {
        commandGateway.sendAndWait<Any>(command)
    }

    override fun storeAIAnalysis(command: StoreAIAnalysisCommand) {
        commandGateway.sendAndWait<Any>(command)
    }

    override fun finalizeScene(command: FinalizeAccidentSceneCommand) {
        commandGateway.sendAndWait<Any>(command)
    }

    override fun archiveScene(command: ArchiveAccidentSceneCommand) {
        commandGateway.sendAndWait<Any>(command)
    }
}