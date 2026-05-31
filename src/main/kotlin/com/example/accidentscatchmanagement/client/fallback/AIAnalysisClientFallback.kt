package com.example.accidentscatchmanagement.client.fallback

import com.example.accidentscatchmanagement.client.AIAnalysisClient
import com.example.accidentscatchmanagement.web.dto.AIAnalysisResponse
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class AIAnalysisClientFallback : AIAnalysisClient {

    override fun analyzeImage(image: MultipartFile): AIAnalysisResponse {
        return AIAnalysisResponse(
            cars = emptyList(),
            confidence = 0.0,
            aiSummary = "AI service is currently unavailable. No vehicles were detected."
        )
    }
}