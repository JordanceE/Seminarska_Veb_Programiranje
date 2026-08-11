package com.example.accidentscatchmanagement.domain.valueobjects

import jakarta.persistence.Embeddable

@Embeddable
data class CanvasPosition(
    var x: Double = 0.0,
    var y: Double = 0.0
)