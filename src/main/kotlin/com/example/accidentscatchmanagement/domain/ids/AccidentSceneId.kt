package com.example.accidentscatchmanagement.domain.ids


import java.util.UUID

data class AccidentSceneId(val value: String = "AccidentScene:${UUID.randomUUID()}") {
    override fun toString(): String = value
}

data class VehicleId(val value: String = "Vehicle:${UUID.randomUUID()}") {
    override fun toString(): String = value
}

data class MeasurementId(val value: String = "Measurement:${UUID.randomUUID()}") {
    override fun toString(): String = value
}