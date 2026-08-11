package com.example.accidentscatchmanagement.web

import com.example.accidentscatchmanagement.service.AccidentSceneDeletionService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accident-scenes")
class AccidentSceneDeletionController(
    private val deletionService: AccidentSceneDeletionService
) {

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAccidentScene(
        @PathVariable id: String
    ) {
        deletionService.deleteById(id)
    }
}