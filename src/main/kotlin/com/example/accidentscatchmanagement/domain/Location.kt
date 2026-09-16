package com.example.accidentscatchmanagement.domain

import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.domain.ids.LocationId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.axonframework.modelling.command.AggregateIdentifier
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "location",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_location_fingerprint",
            columnNames = ["fingerprint"]
        )
    ]
)
class Location(
    @Id
    var id: String = LocationId().value,

    @Column(nullable = false)
    var name: String = "",

    var description: String? = null,

    @Enumerated(EnumType.STRING)
    var roadLayoutType: RoadLayoutType =
        RoadLayoutType.INTERSECTION,

    var backgroundFileName: String? = null,

    var topRoadWidth: Double? = null,
    var topRoadLanes: Int? = null,
    var bottomRoadWidth: Double? = null,
    var bottomRoadLanes: Int? = null,
    var leftRoadWidth: Double? = null,
    var leftRoadLanes: Int? = null,
    var rightRoadWidth: Double? = null,
    var rightRoadLanes: Int? = null,
    var roundaboutDiameter: Double? = null,
    var tJunction: Boolean = false,

    var photoStorageKey: String? = null,
    var photoOriginalName: String? = null,
    var photoContentType: String? = null,
    var photoSize: Long? = null,

    @Column(nullable = false, unique = true, length = 64)
    var fingerprint: String = "",

    var archived: Boolean = false,
    var createdAt: LocalDateTime = LocalDateTime.now()
)
