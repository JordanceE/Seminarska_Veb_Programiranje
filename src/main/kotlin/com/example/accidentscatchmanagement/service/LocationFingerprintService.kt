package com.example.accidentscatchmanagement.service

import com.example.accidentscatchmanagement.web.dto.LocationInput
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class LocationFingerprintService(private val mapper: ObjectMapper) {
    fun fingerprint(location: com.example.accidentscatchmanagement.domain.Location): String = fingerprint(
        LocationInput(name = location.name, description = location.description,
            roadLayoutType = location.roadLayoutType, backgroundFileName = location.backgroundFileName,
            topRoadWidth = location.topRoadWidth, topRoadLanes = location.topRoadLanes,
            bottomRoadWidth = location.bottomRoadWidth, bottomRoadLanes = location.bottomRoadLanes,
            leftRoadWidth = location.leftRoadWidth, leftRoadLanes = location.leftRoadLanes,
            rightRoadWidth = location.rightRoadWidth, rightRoadLanes = location.rightRoadLanes,
            roundaboutDiameter = location.roundaboutDiameter, tJunction = location.tJunction),
        location.photoStorageKey
    )

    fun fingerprint(input: LocationInput, photoHash: String?): String = sha256(
        mapper.writeValueAsBytes(listOf(
            input.name, input.description, input.roadLayoutType.name,
            input.backgroundFileName, input.topRoadWidth, input.topRoadLanes,
            input.bottomRoadWidth, input.bottomRoadLanes, input.leftRoadWidth,
            input.leftRoadLanes, input.rightRoadWidth, input.rightRoadLanes,
            input.roundaboutDiameter, input.tJunction, photoHash
        ))
    )

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
