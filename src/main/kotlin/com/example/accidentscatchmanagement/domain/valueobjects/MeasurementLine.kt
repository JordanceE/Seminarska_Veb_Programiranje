package com.example.accidentscatchmanagement.domain.valueobjects
import com.example.accidentscatchmanagement.domain.enums.MeasurementType
import com.example.accidentscatchmanagement.domain.ids.MeasurementId
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated


@Embeddable
data class MeasurementLine(
    var measurementId: String = MeasurementId().value,
    var fromVehicleId: String? = null,
    var toVehicleId: String? = null,
    @Enumerated(EnumType.STRING)
    var type: MeasurementType = MeasurementType.POINT_TO_POINT,

    var x1: Double = 0.0,
    var y1: Double = 0.0,
    var x2: Double = 0.0,
    var y2: Double = 0.0,

    var lengthMeters: Double = 0.0,
    var label: String? = null
) {
    init {
        require(lengthMeters >= 0) { "Measurement length cannot be negative" }
    }
}