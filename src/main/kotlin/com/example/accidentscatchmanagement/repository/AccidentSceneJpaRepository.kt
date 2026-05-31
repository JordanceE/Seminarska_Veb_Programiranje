package com.example.accidentscatchmanagement.repository

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import org.springframework.data.jpa.repository.JpaRepository

interface AccidentSceneJpaRepository : JpaRepository<AccidentScene, String> {
    fun findAllByStatus(status: SceneStatus): List<AccidentScene>
}