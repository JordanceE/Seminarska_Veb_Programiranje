package com.example.accidentscatchmanagement.repository

import com.example.accidentscatchmanagement.domain.Location
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LocationRepository : JpaRepository<Location, String> {
    fun findAllByNameAndRoadLayoutTypeAndArchivedFalseOrderByCreatedAtAscIdAsc(
        name: String,
        roadLayoutType: com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
    ): List<Location>

    @Query("""
        select l from Location l where l.archived = false and
        (:query = '' or locate(lower(:query), lower(l.name)) > 0
        or locate(lower(:query), lower(coalesce(l.description, ''))) > 0)
    """)
    fun search(@Param("query") query: String, pageable: Pageable): Page<Location>

    fun findByFingerprint(fingerprint: String): Location?

    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable
    ): Page<Location>
}
