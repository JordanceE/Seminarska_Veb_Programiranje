package com.example.accidentscatchmanagement.service.impl

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import com.example.accidentscatchmanagement.repository.AccidentSceneJpaRepository
import com.example.accidentscatchmanagement.service.AccidentSceneQueryService
import org.springframework.stereotype.Service

@Service
class AccidentSceneQueryServiceImpl(
    private val repository: AccidentSceneJpaRepository
) : AccidentSceneQueryService {

    override fun findAll(): List<AccidentScene> =
        repository.findAll()

    override fun findById(id: String): AccidentScene =
        repository.findById(id).orElseThrow {
            IllegalArgumentException("Accident scene with id $id was not found")
        }

    override fun findByStatus(status: SceneStatus): List<AccidentScene> =
        repository.findAllByStatus(status)
}