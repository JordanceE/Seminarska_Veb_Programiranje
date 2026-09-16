package com.example.accidentscatchmanagement.web

import com.example.accidentscatchmanagement.service.LocationService
import com.example.accidentscatchmanagement.web.dto.LocationInput
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.Duration

@RestController
@RequestMapping("/api/locations", "/api/location")
class LocationController(private val locations: LocationService) {
    @GetMapping
    fun search(@RequestParam(defaultValue = "") query: String,
               @PageableDefault(size = 20, sort = ["name", "id"]) pageable: Pageable) =
        locations.search(query, pageable)

    @GetMapping("/{id}")
    fun get(@PathVariable id: String) = locations.get(id)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        locations.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun create(@RequestBody input: LocationInput) = locations.save(input)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestPart("location") input: LocationInput,
               @RequestPart("photo", required = false) photo: MultipartFile?) = locations.save(input, photo)

    @GetMapping("/{id}/photo")
    fun photo(@PathVariable id: String): ResponseEntity<ByteArray> {
        val photo = locations.photo(id)
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(photo.contentType))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
            .header("X-Content-Type-Options", "nosniff")
            .eTag(photo.id).body(photo.bytes)
    }
}
