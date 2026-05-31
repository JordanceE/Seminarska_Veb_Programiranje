package com.example.accidentscatchmanagement.client

import com.example.accidentscatchmanagement.client.fallback.AIAnalysisClientFallback
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.MediaType
import org.springframework.cloud.openfeign.FeignClient
import com.example.accidentscatchmanagement.web.dto.AIAnalysisResponse

@FeignClient(
    name = "accident-ai-service",
    url = "\${roadwatch.ai-service.url}",
    fallback = AIAnalysisClientFallback::class
)
interface AIAnalysisClient {

    @PostMapping(
        value = ["/analyze"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun analyzeImage(
        @RequestPart("image") image: MultipartFile
    ): AIAnalysisResponse
}