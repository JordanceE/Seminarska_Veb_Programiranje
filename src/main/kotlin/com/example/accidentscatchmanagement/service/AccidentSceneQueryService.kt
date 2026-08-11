package com.example.accidentscatchmanagement.service

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import com.example.accidentscatchmanagement.web.dto.AccidentSceneResponse
import com.example.accidentscatchmanagement.web.dto.AccidentSceneSummaryResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AccidentSceneQueryService {

    fun findAll(
        pageable: Pageable
    ): Page<AccidentSceneSummaryResponse>
    fun search(
        query: String?,
        status: SceneStatus?,
        pageable: Pageable
    ): Page<AccidentSceneSummaryResponse>
    fun findById(id: String): AccidentSceneResponse

    fun findByStatus(
        status: SceneStatus
    ): List<AccidentSceneSummaryResponse>
}