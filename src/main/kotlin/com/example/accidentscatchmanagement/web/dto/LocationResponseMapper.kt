package com.example.accidentscatchmanagement.web.dto

import com.example.accidentscatchmanagement.domain.Location
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets

fun Location.toLocationResponse() = LocationResponse(
    id = id, name = name, description = description,
    roadLayoutType = roadLayoutType, backgroundFileName = backgroundFileName,
    topRoadWidth = topRoadWidth, topRoadLanes = topRoadLanes,
    bottomRoadWidth = bottomRoadWidth, bottomRoadLanes = bottomRoadLanes,
    leftRoadWidth = leftRoadWidth, leftRoadLanes = leftRoadLanes,
    rightRoadWidth = rightRoadWidth, rightRoadLanes = rightRoadLanes,
    roundaboutDiameter = roundaboutDiameter, tJunction = tJunction,
    photoUrl = photoStorageKey?.let {
        "/api/locations/${UriUtils.encodePathSegment(id, StandardCharsets.UTF_8)}/photo"
    }
)
