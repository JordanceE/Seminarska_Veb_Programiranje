package com.example.accidentscatchmanagement.web

import com.example.accidentscatchmanagement.client.AIAnalysisClient
import com.example.accidentscatchmanagement.domain.commands.StoreAIAnalysisCommand
import com.example.accidentscatchmanagement.service.AccidentSceneCommandService
import com.example.accidentscatchmanagement.web.dto.AIAnalysisResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/accident-scenes")
class AIAnalysisController(
    private val aiAnalysisClient: AIAnalysisClient,
    private val commandService: AccidentSceneCommandService
) {

    @PostMapping("/{id}/analyze")
    fun analyzeAndStore(
        @PathVariable id: String,
        @RequestPart("image") image: MultipartFile
    ): AIAnalysisResponse {
        val response = aiAnalysisClient.analyzeImage(image)

        commandService.storeAIAnalysis(
            StoreAIAnalysisCommand(
                accidentSceneId = id,
                detectedVehicles = response.cars,
                confidence = response.confidence,
                summary = response.aiSummary
            )
        )

        return response
    }
}