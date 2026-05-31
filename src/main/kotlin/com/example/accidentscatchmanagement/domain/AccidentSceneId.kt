package com.example.accidentscatchmanagement.domain

import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
data class AccidentSceneId(
    val value: String = "AccidentScene:${UUID.randomUUID()}"
) {
    override fun toString(): String = value
}