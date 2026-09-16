package com.example.accidentscatchmanagement

import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.repository.LocationRepository
import com.example.accidentscatchmanagement.service.LocationService
import com.example.accidentscatchmanagement.web.dto.LocationInput
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.imageio.ImageIO

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:location-tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "axon.eventhandling.processors.default.mode=subscribing"
])
@AutoConfigureMockMvc
class LocationWorkflowTests {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var locations: LocationService
    @Autowired lateinit var repository: LocationRepository
    @Autowired lateinit var jdbc: org.springframework.jdbc.core.JdbcTemplate
    @Autowired lateinit var migration: com.example.accidentscatchmanagement.config.LegacyLocationMigration
    @Autowired lateinit var fileNameMigration: com.example.accidentscatchmanagement.config.SceneFileNameMigration
    @Autowired lateinit var commands: com.example.accidentscatchmanagement.service.AccidentSceneCommandService

    private fun input() = LocationInput("Location ${UUID.randomUUID()}", topRoadWidth = 7.0,
        topRoadLanes = 2, backgroundFileName = "glavnaulica.png")
    private fun postJson(path: String, body: Any) = mapper.readTree(mvc.perform(post(path)
        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)))
        .andExpect(status().is2xxSuccessful).andReturn().response.contentAsString)
    private fun scene(locationId: String? = null, fileName: String? = null) = postJson("/api/accident-scenes",
        mapOf("roadLayoutType" to "INTERSECTION", "locationId" to locationId, "fileName" to fileName))["id"].asText()
    private fun getJson(path: String) = mapper.readTree(mvc.perform(get(path))
        .andExpect(status().isOk).andReturn().response.contentAsString)

    @Test fun `identical normalized input reuses one location even concurrently`() {
        val input = input()
        val executor = Executors.newFixedThreadPool(2)
        try {
            val ids = executor.invokeAll(listOf(Callable { locations.save(input).id },
                Callable { locations.save(input.copy(name = " ${input.name} ")).id })).map { it.get() }
            assertEquals(ids[0], ids[1])
            assertNotEquals(ids[0], locations.save(input.copy(topRoadWidth = 8.0)).id)
        } finally { executor.shutdownNow() }
    }

    @Test fun `accident file names are independent of shared locations and searchable with other filters`() {
        val input = input()
        val location = locations.save(input)
        val token = UUID.randomUUID().toString()
        val first = scene(location.id, "  Report-$token-A  ")
        val second = scene(location.id, "Report-$token-B")
        assertEquals("Report-$token-A", getJson("/api/accident-scenes/$first")["fileName"].asText())
        assertFalse(getJson("/api/locations/${location.id}").has("fileName"))
        assertFalse(getJson("/api/accident-scenes/$first")["locationInfo"].has("fileName"))
        fun save(fileName: String?) {
            mvc.perform(put("/api/accident-scenes/$first/full-scene").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(mapOf("roadLayoutType" to "INTERSECTION",
                    "locationId" to location.id, "vehicles" to emptyList<Any>(), "fileName" to fileName))))
                .andExpect(status().isOk)
        }
        save("Renamed-$token")
        save(null) // Older callers without this field must not erase the name.
        assertEquals("Renamed-$token", getJson("/api/accident-scenes/$first")["fileName"].asText())
        assertEquals("Report-$token-B", getJson("/api/accident-scenes/$second")["fileName"].asText())
        assertEquals(location.id, getJson("/api/accident-scenes/$first")["locationId"].asText())
        assertEquals(location.id, locations.save(input).id)
        postJson("/api/accident-scenes/$first/vehicles", mapOf("vehicle" to mapOf("name" to "V1", "plate" to "SK-4567 AB")))
        val match = getJson("/api/accident-scenes?query=renamed-$token&locationId=${location.id}&plate=sk4567ab&status=DRAFT")
        assertEquals(1, match["totalElements"].asInt())
        assertEquals("Renamed-$token", match["content"][0]["fileName"].asText())
        assertEquals("Renamed-$token", match["content"][0]["name"].asText())
        assertEquals(location.name, match["content"][0]["locationName"].asText())
        val projected = getJson("/api/accident-scenes/by-status/DRAFT").first { it["id"].asText() == first }
        assertEquals("Renamed-$token", projected["fileName"].asText())
        assertEquals(location.name, projected["locationName"].asText())
        val payloads = jdbc.query("select payload from domain_event_entry where aggregate_identifier = ?",
            org.springframework.jdbc.core.RowMapper { rs, _ -> String(rs.getBytes(1), Charsets.UTF_8) }, first)
        assertTrue(payloads.any { it.contains("Renamed-$token") })
        mvc.perform(post("/api/accident-scenes").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(mapOf("roadLayoutType" to "INTERSECTION", "fileName" to "x".repeat(256)))))
            .andExpect(status().isBadRequest)
        save("")
        assertTrue(getJson("/api/accident-scenes/$first")["fileName"].isNull)
    }

    @Test fun `file name migration preserves existing names and does not repeat after clearing`() {
        val location = locations.save(input())
        val first = scene(location.id)
        val second = scene(location.id, "Already named")
        val unlocated = scene(fileName = "File without location")
        jdbc.execute("alter table location add column if not exists file_name varchar(255)")
        jdbc.update("update location set file_name = ? where id = ?", "Legacy report", location.id)
        jdbc.update("delete from roadwatch_schema_migration where version = 'scene-file-name-v1'")
        fileNameMigration.run(org.springframework.boot.DefaultApplicationArguments())
        assertEquals("Legacy report", getJson("/api/accident-scenes/$first")["fileName"].asText())
        assertEquals("Already named", getJson("/api/accident-scenes/$second")["fileName"].asText())
        assertEquals("File without location", getJson("/api/accident-scenes/$unlocated")["fileName"].asText())
        jdbc.update("update accident_scene set file_name = null where id = ?", first)
        fileNameMigration.run(org.springframework.boot.DefaultApplicationArguments())
        assertTrue(getJson("/api/accident-scenes/$first")["fileName"].isNull)
        migration.run(org.springframework.boot.DefaultApplicationArguments())
        assertTrue(getJson("/api/accident-scenes/$unlocated")["locationId"].isNull)
    }

    @Test fun `legacy location fingerprints still reuse the same template without file names`() {
        val input = input()
        val saved = locations.save(input)
        // Simulate the old fingerprint algorithm, which included the location's file name.
        jdbc.update("update location set fingerprint = ? where id = ?", "legacy-${UUID.randomUUID()}", saved.id)
        assertEquals(saved.id, locations.save(input).id)
        assertNotEquals(saved.id, locations.save(input.copy(topRoadWidth = 11.0)).id)
    }

    @Test fun `photo and details survive reads and duplicate uploads reuse template`() {
        val input = input()
        val bytes = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()
        fun upload() = mapper.readTree(mvc.perform(multipart("/api/locations")
            .file(MockMultipartFile("location", "location.json", "application/json", mapper.writeValueAsBytes(input)))
            .file(MockMultipartFile("photo", "layout.png", "image/png", bytes)))
            .andExpect(status().isOk).andReturn().response.contentAsString)
        val first = upload()
        assertEquals(first["id"], upload()["id"])
        assertArrayEquals(bytes, mvc.perform(get(first["photoUrl"].asText())).andExpect(status().isOk)
            .andReturn().response.contentAsByteArray)
        val copy = locations.save(input.copy(name = "Copy ${UUID.randomUUID()}", photoSourceLocationId = first["id"].asText()))
        assertArrayEquals(bytes, locations.photo(copy.id).bytes)
        mvc.perform(multipart("/api/locations")
            .file(MockMultipartFile("location", "location.json", "application/json", mapper.writeValueAsBytes(input)))
            .file(MockMultipartFile("photo", "fake.png", "image/png", "not a picture".toByteArray())))
            .andExpect(status().isBadRequest)
        mvc.perform(delete("/api/locations/${first["id"].asText()}")).andExpect(status().isNoContent)
        mvc.perform(get(first["photoUrl"].asText())).andExpect(status().isNotFound)
        assertArrayEquals(bytes, locations.photo(copy.id).bytes)
    }

    @Test fun `unused location can be deleted and its values saved again`() {
        val input = input()
        val location = locations.save(input)
        mvc.perform(delete("/api/locations/${location.id}")).andExpect(status().isNoContent)
        assertFalse(repository.existsById(location.id))
        mvc.perform(get("/api/locations/${location.id}")).andExpect(status().isNotFound)
        mvc.perform(delete("/api/locations/${location.id}")).andExpect(status().isNotFound)
        assertEquals(0, getJson("/api/locations?query=${input.name}")["totalElements"].asInt())
        assertNotEquals(location.id, locations.save(input).id)
    }

    @Test fun `deleting a shared location is blocked including for archived accidents`() {
        val location = locations.save(input())
        val first = scene(location.id)
        val second = scene(location.id)
        mvc.perform(delete("/api/locations/${location.id}")).andExpect(status().isConflict)
        for (id in listOf(first, second)) {
            postJson("/api/accident-scenes/$id/vehicles", mapOf("vehicle" to mapOf("name" to "V1")))
            mvc.perform(post("/api/accident-scenes/$id/finalize")).andExpect(status().isOk)
            mvc.perform(post("/api/accident-scenes/$id/archive")).andExpect(status().isOk)
        }
        val response = mvc.perform(delete("/api/locations/${location.id}"))
            .andExpect(status().isConflict).andReturn().response
        assertTrue(mapper.readTree(response.contentAsString)["message"].asText().contains("2 saved accident"))
        assertTrue(repository.existsById(location.id))
        for (id in listOf(first, second)) {
            assertEquals(location.id, getJson("/api/accident-scenes/$id")["locationId"].asText())
            assertEquals(location.name, getJson("/api/accident-scenes/$id")["location"]["name"].asText())
        }
        // The database constraint remains a second guard if a concurrent request links a scene.
        assertThrows(org.springframework.dao.DataIntegrityViolationException::class.java) {
            jdbc.update("delete from location where id = ?", location.id)
        }
    }

    @Test fun `scenes share a location and manual full scene saves resolve a separate template`() {
        val location = locations.save(input())
        val first = scene(location.id)
        val second = scene(location.id)
        assertEquals(location.id, getJson("/api/accident-scenes/$first")["locationId"].asText())
        mvc.perform(put("/api/accident-scenes/$first/full-scene").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(mapOf("roadLayoutType" to "INTERSECTION",
                "locationInfo" to input().copy(name = "Edited ${UUID.randomUUID()}"),
                "vehicles" to emptyList<Any>(), "measurements" to emptyList<Any>()))))
            .andExpect(status().isOk)
        assertNotEquals(location.id, getJson("/api/accident-scenes/$first")["locationId"].asText())
        assertEquals(location.id, getJson("/api/accident-scenes/$second")["locationId"].asText())
        assertEquals(location.name, locations.get(location.id).name)
    }

    @Test fun `plate and location filters combine and do not duplicate accidents`() {
        val location = locations.save(input())
        val other = locations.save(input())
        val first = scene(location.id)
        val second = scene(other.id)
        val blank = scene()
        for (id in listOf(first, second)) repeat(2) {
            postJson("/api/accident-scenes/$id/vehicles", mapOf("vehicle" to mapOf(
                "name" to "V$it", "plate" to "SK-9876 AB")))
        }
        val matches = getJson("/api/accident-scenes?plate=sk9876ab&locationId=${location.id}&size=1")
        assertEquals(1, matches["totalElements"].asInt())
        assertEquals(first, matches["content"][0]["id"].asText())
        assertTrue(getJson("/api/accident-scenes/$blank")["locationId"].isNull)
        val unlocated = getJson("/api/accident-scenes?query=$blank")
        assertEquals(1, unlocated["totalElements"].asInt())
        assertFalse(getJson("/api/accident-scenes/$first")["locationInfo"]["tJunction"].asBoolean())
    }

    @Test fun `invalid location reference and invalid measurements are rejected`() {
        mvc.perform(post("/api/accident-scenes").contentType(MediaType.APPLICATION_JSON)
            .content("""{"roadLayoutType":"INTERSECTION","locationId":"missing"}"""))
            .andExpect(status().isNotFound)
        mvc.perform(post("/api/locations").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(input().copy(topRoadWidth = -1.0))))
            .andExpect(status().isBadRequest)
        mvc.perform(post("/api/locations").contentType(MediaType.APPLICATION_JSON)
            .content("""{"name":"Invalid","topRoadLanes":1.5}"""))
            .andExpect(status().isBadRequest)
    }

    @Test fun `legacy embedded fields are migrated once and retained`() {
        val sceneId = scene()
        jdbc.execute("alter table accident_scene add column if not exists name varchar(255)")
        jdbc.execute("alter table accident_scene add column if not exists top_road_width double precision")
        jdbc.execute("alter table accident_scene add column if not exists t_junction boolean")
        val name = "Legacy ${UUID.randomUUID()}"
        jdbc.update("update accident_scene set name = ?, top_road_width = 8, t_junction = true where id = ?", name, sceneId)
        val arguments = org.springframework.boot.DefaultApplicationArguments()
        migration.run(arguments)
        val migrated = getJson("/api/accident-scenes/$sceneId")
        assertEquals(name, migrated["location"]["name"].asText())
        assertEquals(8.0, migrated["location"]["topRoadWidth"].asDouble())
        assertTrue(migrated["locationInfo"]["tJunction"].asBoolean())
        migration.run(arguments)
        assertEquals(migrated["locationId"], getJson("/api/accident-scenes/$sceneId")["locationId"])
        assertEquals(name, jdbc.queryForObject("select name from accident_scene where id = ?", String::class.java, sceneId))
    }

    @Test fun `AI results preserve the shared location and persist measurements and events`() {
        val location = locations.save(input())
        val id = scene(location.id, "AI report")
        val vehicle = com.example.accidentscatchmanagement.domain.valueobjects.VehiclePlacement(name = "V1")
        val measurement = com.example.accidentscatchmanagement.domain.valueobjects.MeasurementLine(
            fromVehicleId = vehicle.vehicleId, x1 = 0.0, y1 = 0.0, x2 = 10.0, y2 = 0.0, lengthMeters = 1.0)
        commands.storeAIAnalysis(com.example.accidentscatchmanagement.domain.commands.StoreAIAnalysisCommand(
            id, listOf(vehicle), listOf(measurement), 0.9, "Detected one vehicle"))
        val result = getJson("/api/accident-scenes/$id")
        assertEquals(location.id, result["locationId"].asText())
        assertEquals("AI report", result["fileName"].asText())
        assertEquals(location.name, result["location"]["name"].asText())
        assertEquals(1, result["measurements"].size())
        assertEquals(1.0, result["measurements"][0]["lengthMeters"].asDouble())
        assertTrue(jdbc.queryForObject("select count(*) from domain_event_entry where aggregate_identifier = ?",
            Long::class.java, id)!! >= 2)
    }

    @Test fun `changing selected location also changes its road layout`() {
        val id = scene()
        val location = locations.save(input().copy(roadLayoutType = RoadLayoutType.T_JUNCTION,
            backgroundFileName = "t-junction.png", tJunction = true))
        mvc.perform(patch("/api/accident-scenes/$id/location").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(mapOf("locationId" to location.id))))
            .andExpect(status().isOk)
        val result = getJson("/api/accident-scenes/$id")
        assertEquals("T_JUNCTION", result["roadLayoutType"].asText())
        assertEquals(location.id, result["locationId"].asText())
    }
}
