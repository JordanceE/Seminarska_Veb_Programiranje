package com.example.accidentscatchmanagement.repository.projection

import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import java.time.LocalDateTime

interface AccidentSceneSummaryProjection {
    val id: String
    val roadLayoutType: RoadLayoutType
    val name: String?
    val locationName: String?
    val locationId: String?
    val status: SceneStatus
    val aiConfidence: Double?
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
}
