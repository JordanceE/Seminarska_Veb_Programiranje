package com.example.accidentscatchmanagement.web.dto

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.repository.projection.AccidentSceneSummaryProjection

fun AccidentScene.toResponse(): AccidentSceneResponse {
    return AccidentSceneResponse(
        id = id,
        roadLayoutType = roadLayoutType,

        locationInfo = LocationInfoResponse(
            fileName = locationInfo.fileName,
            name = locationInfo.name,
            description = locationInfo.description,
            topRoadWidth = locationInfo.topRoadWidth,
            topRoadLanes = locationInfo.topRoadLanes,
            bottomRoadWidth = locationInfo.bottomRoadWidth,
            bottomRoadLanes = locationInfo.bottomRoadLanes,
            leftRoadWidth = locationInfo.leftRoadWidth,
            leftRoadLanes = locationInfo.leftRoadLanes,
            rightRoadWidth = locationInfo.rightRoadWidth,
            rightRoadLanes = locationInfo.rightRoadLanes,
            roundaboutDiameter = locationInfo.roundaboutDiameter,
            tJunction = locationInfo.tJunction
        ),

        status = status,
        aiConfidence = aiConfidence,
        aiSummary = aiSummary,
        createdAt = createdAt,
        updatedAt = updatedAt,

        vehicles = vehicles.map { vehicle ->
            VehiclePlacementResponse(
                vehicleId = vehicle.vehicleId,
                name = vehicle.name,
                type = vehicle.type,
                model = vehicle.model,
                color = vehicle.color,
                plate = vehicle.plate,
                guilty = vehicle.guilty,
                comment = vehicle.comment,

                position = CanvasPositionResponse(
                    x = vehicle.position.x,
                    y = vehicle.position.y
                ),

                width = vehicle.width,
                height = vehicle.height,
                rotation = vehicle.rotation,
                scale = vehicle.scale,
                flipped = vehicle.flipped,
                note = vehicle.note,
                confidence = vehicle.confidence
            )
        },

        measurements = measurements.map { measurement ->
            MeasurementLineResponse(
                measurementId = measurement.measurementId,
                fromVehicleId = measurement.fromVehicleId,
                toVehicleId = measurement.toVehicleId,
                type = measurement.type,
                x1 = measurement.x1,
                y1 = measurement.y1,
                x2 = measurement.x2,
                y2 = measurement.y2,
                lengthMeters = measurement.lengthMeters,
                label = measurement.label
            )
        }
    )
}
fun AccidentScene.toSummaryResponse(): AccidentSceneSummaryResponse {
    return AccidentSceneSummaryResponse(
        id = id,
        roadLayoutType = roadLayoutType,
        name = locationInfo.name,
        status = status,
        aiConfidence = aiConfidence,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
fun AccidentSceneSummaryProjection.toResponse():
        AccidentSceneSummaryResponse {

    return AccidentSceneSummaryResponse(
        id = id,
        roadLayoutType = roadLayoutType,
        name = name,
        status = status,
        aiConfidence = aiConfidence,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}