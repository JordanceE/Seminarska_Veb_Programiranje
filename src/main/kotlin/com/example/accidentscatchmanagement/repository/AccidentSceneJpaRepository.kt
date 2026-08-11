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

    fun findAllByStatus(
        status: SceneStatus
    ): List<AccidentScene>
    @Query(
        """
    select a
    from AccidentScene a
    where
        (:status is null or a.status = :status)
        and (
            :query is null
            or :query = ''
            or lower(a.id) like lower(concat('%', :query, '%'))
            or lower(coalesce(a.locationInfo.name, ''))
                like lower(concat('%', :query, '%'))
            or lower(coalesce(a.locationInfo.fileName, ''))
                like lower(concat('%', :query, '%'))
            or lower(coalesce(a.locationInfo.description, ''))
                like lower(concat('%', :query, '%'))
        )
    """
    )
    fun search(
        @Param("query") query: String?,
        @Param("status") status: SceneStatus?,
        pageable: Pageable
    ): Page<AccidentScene>
    @Query(
        """
        select
            a.id as id,
            a.roadLayoutType as roadLayoutType,
            a.locationInfo.name as name,
            a.status as status,
            a.aiConfidence as aiConfidence,
            a.createdAt as createdAt,
            a.updatedAt as updatedAt
        from AccidentScene a
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
            a.locationInfo.name as name,
            a.status as status,
            a.aiConfidence as aiConfidence,
            a.createdAt as createdAt,
            a.updatedAt as updatedAt
        from AccidentScene a
        where a.status = :status
        order by a.updatedAt desc
        """
    )
    fun findSummariesByStatus(
        @Param("status") status: SceneStatus
    ): List<AccidentSceneSummaryProjection>
}