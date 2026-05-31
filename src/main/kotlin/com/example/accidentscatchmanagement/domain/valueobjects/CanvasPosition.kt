package com.example.accidentscatchmanagement.domain.valueobjects

import jakarta.persistence.Embeddable

@Embeddable
data class CanvasPosition(
    var x: Double = 0.0,
    var y: Double = 0.0
) {
    init {
        require(x >= 0) { "X coordinate cannot be negative" }
        require(y >= 0) { "Y coordinate cannot be negative" }
    }
}