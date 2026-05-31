package com.example.accidentscatchmanagement.domain.valueobjects

import com.example.accidentscatchmanagement.domain.enums.VehicleType
import com.example.accidentscatchmanagement.domain.ids.VehicleId
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class VehiclePlacement(
    var vehicleId: String = VehicleId().value,

    var name: String = "",

    @Enumerated(EnumType.STRING)
    var type: VehicleType = VehicleType.SEDAN,

    var model: String? = null,
    var color: String? = null,
    var plate: String? = null,
    var guilty: Boolean = false,
    var comment: String? = null,

    @Embedded
    var position: CanvasPosition = CanvasPosition(),

    var width: Double = 60.0,
    var height: Double = 20.0,
    var rotation: Double = 0.0,
    var scale: Double = 1.0,
    var flipped: Boolean = false,
    var note: String? = null
) {
    init {
        require(name.isNotBlank()) { "Vehicle name cannot be blank" }
        require(width > 0) { "Vehicle width must be positive" }
        require(height > 0) { "Vehicle height must be positive" }
        require(scale > 0) { "Vehicle scale must be positive" }
    }
}