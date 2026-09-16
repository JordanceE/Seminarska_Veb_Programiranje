package com.example.accidentscatchmanagement.web.dto

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.repository.projection.AccidentSceneSummaryProjection

fun AccidentScene.toResponse(): AccidentSceneResponse {
    return AccidentSceneResponse(
        id = id,
        fileName = fileName,
        roadLayoutType = roadLayoutType,

        locationInfo = LocationInfoResponse(
            name = location?.name,
            description = location?.description,
            topRoadWidth = location?.topRoadWidth,
            topRoadLanes = location?.topRoadLanes,
            bottomRoadWidth = location?.bottomRoadWidth,
            bottomRoadLanes = location?.bottomRoadLanes,
            leftRoadWidth = location?.leftRoadWidth,
            leftRoadLanes = location?.leftRoadLanes,
            rightRoadWidth = location?.rightRoadWidth,
            rightRoadLanes = location?.rightRoadLanes,
            roundaboutDiameter = location?.roundaboutDiameter,
            tJunction = location?.tJunction ?: false
        ),

        locationId = locationId,
        location = location?.toLocationResponse(),
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
        name = fileName,
        fileName = fileName,
        locationName = location?.name,
        locationId = locationId,
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
        fileName = name,
        locationName = locationName,
        locationId = locationId,
        status = status,
        aiConfidence = aiConfidence,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
