package com.example.accidentscatchmanagement.web

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import com.example.accidentscatchmanagement.service.AccidentSceneQueryService
import com.example.accidentscatchmanagement.web.dto.AccidentSceneResponse
import com.example.accidentscatchmanagement.web.dto.AccidentSceneSummaryResponse

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.RequestParam

@RestController
@RequestMapping("/api/accident-scenes")
class AccidentSceneQueryController(
    private val queryService: AccidentSceneQueryService
) {

    @GetMapping
    fun findAll(
        @RequestParam(required = false)
        query: String?,

        @RequestParam(required = false)
        status: SceneStatus?,

        @RequestParam(required = false) plate: String?,
        @RequestParam(required = false) locationId: String?,

        pageable: Pageable
    ): Page<AccidentSceneSummaryResponse> {
        return queryService.search(
            query = query,
            status = status,
            plate = plate,
            locationId = locationId,
            pageable = pageable
        )
    }

    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: String
    ): AccidentSceneResponse {
        return queryService.findById(id)
    }

    @GetMapping("/by-status/{status}")
    fun findByStatus(
        @PathVariable status: SceneStatus
    ): List<AccidentSceneSummaryResponse> {
        return queryService.findByStatus(status)
    }
}
