package com.example.accidentscatchmanagement.repository

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import com.example.accidentscatchmanagement.repository.projection.AccidentSceneSummaryProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AccidentSceneJpaRepository :
    JpaRepository<AccidentScene, String> {

    fun countByLocationId(locationId: String): Long

    fun findAllByStatus(
        status: SceneStatus
    ): List<AccidentScene>
    @Query(
        """
    select a
    from AccidentScene a
    left join a.location l
    where
        (:status is null or a.status = :status)
        and (:locationId = '' or a.locationId = :locationId)
        and (:plate = '' or exists (
            select 1 from AccidentScene s join s.vehicles v
            where s.id = a.id
            and locate(:plate, lower(replace(replace(coalesce(v.plate, ''), ' ', ''), '-', ''))) > 0
        ))
        and (
            :query is null
            or :query = ''
            or locate(lower(:query), lower(a.id)) > 0
            or locate(lower(:query), lower(coalesce(a.fileName, ''))) > 0
        )
    """
    )
    fun search(
        @Param("query") query: String?,
        @Param("status") status: SceneStatus?,
        @Param("plate") plate: String,
        @Param("locationId") locationId: String,
        pageable: Pageable
    ): Page<AccidentScene>
    @Query(
        """
        select
            a.id as id,
            a.roadLayoutType as roadLayoutType,
            a.fileName as name,
            l.name as locationName,
            a.locationId as locationId,
            a.status as status,
            a.aiConfidence as aiConfidence,
            a.createdAt as createdAt,
            a.updatedAt as updatedAt
        from AccidentScene a
        left join a.location l
        order by a.updatedAt desc
        """
    )
    fun findAllSummaries():
            List<AccidentSceneSummaryProjection>

    @Query(
        """
        select
            a.id as id,
            a.roadLayoutType as roadLayoutType,
            a.fileName as name,
            l.name as locationName,
            a.locationId as locationId,
            a.status as status,
            a.aiConfidence as aiConfidence,
            a.createdAt as createdAt,
            a.updatedAt as updatedAt
        from AccidentScene a
        left join a.location l
        where a.status = :status
        order by a.updatedAt desc
        """
    )
    fun findSummariesByStatus(
        @Param("status") status: SceneStatus
    ): List<AccidentSceneSummaryProjection>
}
