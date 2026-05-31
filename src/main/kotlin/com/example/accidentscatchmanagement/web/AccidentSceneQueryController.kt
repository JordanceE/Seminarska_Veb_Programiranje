package com.example.accidentscatchmanagement.web

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import com.example.accidentscatchmanagement.service.AccidentSceneQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accident-scenes")
class AccidentSceneQueryController(
    private val queryService: AccidentSceneQueryService
) {

    @GetMapping
    fun findAll(): List<AccidentScene> =
        queryService.findAll()

    @GetMapping("/{id}")
    fun findById(@PathVariable id: String): AccidentScene =
        queryService.findById(id)

    @GetMapping("/by-status/{status}")
    fun findByStatus(@PathVariable status: SceneStatus): List<AccidentScene> =
        queryService.findByStatus(status)
}