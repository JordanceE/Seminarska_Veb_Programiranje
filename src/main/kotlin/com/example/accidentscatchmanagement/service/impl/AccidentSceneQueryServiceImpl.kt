package com.example.accidentscatchmanagement.service.impl

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import com.example.accidentscatchmanagement.exceptions.AccidentSceneNotFoundException
import com.example.accidentscatchmanagement.repository.AccidentSceneJpaRepository
import com.example.accidentscatchmanagement.service.AccidentSceneQueryService
import com.example.accidentscatchmanagement.web.dto.AccidentSceneResponse
import com.example.accidentscatchmanagement.web.dto.AccidentSceneSummaryResponse
import com.example.accidentscatchmanagement.web.dto.toResponse
import com.example.accidentscatchmanagement.web.dto.toSummaryResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Service
@Transactional(readOnly = true)
class AccidentSceneQueryServiceImpl(
    private val repository: AccidentSceneJpaRepository
) : AccidentSceneQueryService {

    override fun findAll(
        pageable: Pageable
    ): Page<AccidentSceneSummaryResponse> {
        return repository.findAll(pageable)
            .map { scene ->
                scene.toSummaryResponse()
            }
    }
    override fun findById(
        id: String
    ): AccidentSceneResponse {
        val scene = repository.findById(id)
            .orElseThrow {
                AccidentSceneNotFoundException(id)
            }

        return scene.toResponse()
    }
    override fun search(
        query: String?,
        status: SceneStatus?,
        plate: String?,
        locationId: String?,
        pageable: Pageable
    ): Page<AccidentSceneSummaryResponse> {
        val normalizedQuery = query
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return repository.search(
            query = normalizedQuery,
            status = status,
            plate = plate.orEmpty().lowercase(java.util.Locale.ROOT).replace(" ", "").replace("-", ""),
            locationId = locationId?.trim().orEmpty(),
            pageable = pageable
        ).map { scene ->
            scene.toSummaryResponse()
        }
    }

    override fun findByStatus(
        status: SceneStatus
    ): List<AccidentSceneSummaryResponse> {
        return repository.findSummariesByStatus(status)
            .map { projection ->
                projection.toResponse()
            }
    }
}
