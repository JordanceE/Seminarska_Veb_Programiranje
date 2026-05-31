package com.example.accidentscatchmanagement.service

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.domain.enums.SceneStatus

interface AccidentSceneQueryService {
    fun findAll(): List<AccidentScene>
    fun findById(id: String): AccidentScene
    fun findByStatus(status: SceneStatus): List<AccidentScene>
}