package com.example.accidentscatchmanagement.service

import com.example.accidentscatchmanagement.domain.LocationPhoto
import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.web.dto.LocationInput
import com.example.accidentscatchmanagement.web.dto.LocationResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.multipart.MultipartFile

interface LocationService {
    fun search(query: String, pageable: Pageable): Page<LocationResponse>
    fun get(id: String): LocationResponse
    fun delete(id: String)
    fun save(input: LocationInput, photo: MultipartFile? = null): LocationResponse
    fun resolve(id: String?, input: LocationInput?, layout: RoadLayoutType? = null): String?
    fun photo(id: String): LocationPhoto
}
