package com.example.accidentscatchmanagement.config

import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.service.LocationService
import com.example.accidentscatchmanagement.web.dto.LocationInput
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.core.annotation.Order

/** Idempotent backfill of the former embedded fields. Old columns and events are retained. */
@Component
@Order(10)
class LegacyLocationMigration(private val jdbc: JdbcTemplate, private val locations: LocationService) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        jdbc.update("update accident_scene set location_id = null where location_id = 'null'")
        val columns = jdbc.query("select * from accident_scene where 1 = 0", org.springframework.jdbc.core.ResultSetExtractor { rs ->
            (1..rs.metaData.columnCount).map { rs.metaData.getColumnName(it).lowercase() }.toSet()
        }).orEmpty()
        if ("name" !in columns || "top_road_width" !in columns) return
        var migrated = 0
        jdbc.queryForList("select * from accident_scene where location_id is null").forEach { raw ->
            val row = raw.mapKeys { it.key.lowercase() }
            fun text(key: String) = row[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            fun number(key: String) = (row[key] as? Number)?.toDouble()?.takeIf { it > 0 && it.isFinite() }
            fun lanes(key: String) = (row[key] as? Number)?.toInt()?.takeIf { it > 0 }
            val meaningful = listOf("name", "description", "top_road_width", "bottom_road_width",
                "left_road_width", "right_road_width", "roundabout_diameter", "top_road_lanes",
                "bottom_road_lanes", "left_road_lanes", "right_road_lanes").any { text(it) != null }
            if (!meaningful && row["t_junction"] != true) return@forEach
            val saved = locations.save(LocationInput(
                name = text("name") ?: "Imported location",
                description = text("description"),
                roadLayoutType = RoadLayoutType.valueOf(text("road_layout_type") ?: "INTERSECTION"),
                topRoadWidth = number("top_road_width"), topRoadLanes = lanes("top_road_lanes"),
                bottomRoadWidth = number("bottom_road_width"), bottomRoadLanes = lanes("bottom_road_lanes"),
                leftRoadWidth = number("left_road_width"), leftRoadLanes = lanes("left_road_lanes"),
                rightRoadWidth = number("right_road_width"), rightRoadLanes = lanes("right_road_lanes"),
                roundaboutDiameter = number("roundabout_diameter"), tJunction = row["t_junction"] == true
            ))
            migrated += jdbc.update("update accident_scene set location_id = ? where id = ? and location_id is null",
                saved.id, row["id"])
        }
        LoggerFactory.getLogger(javaClass).info("Linked {} legacy scenes to reusable locations", migrated)
    }
}
