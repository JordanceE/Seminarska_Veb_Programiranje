package com.example.accidentscatchmanagement.service.impl

import com.example.accidentscatchmanagement.domain.Location
import com.example.accidentscatchmanagement.domain.LocationPhoto
import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.repository.LocationRepository
import com.example.accidentscatchmanagement.repository.LocationPhotoRepository
import com.example.accidentscatchmanagement.repository.AccidentSceneJpaRepository
import com.example.accidentscatchmanagement.service.LocationService
import com.example.accidentscatchmanagement.service.LocationFingerprintService
import com.example.accidentscatchmanagement.web.dto.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@Service
class LocationServiceImpl(
    private val locations: LocationRepository,
    private val photos: LocationPhotoRepository,
    private val scenes: AccidentSceneJpaRepository,
    private val fingerprints: LocationFingerprintService,
    transactionManager: PlatformTransactionManager
) : LocationService {
    private val transaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    override fun search(query: String, pageable: Pageable): Page<LocationResponse> =
        locations.search(query.trim(), pageable).map { it.toLocationResponse() }

    override fun get(id: String): LocationResponse = find(id).toLocationResponse()

    override fun delete(id: String) {
        try {
            transaction.executeWithoutResult {
                val location = find(id)
                val sceneCount = scenes.countByLocationId(id)
                if (sceneCount > 0) {
                    throw ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot delete this location: it is used by $sceneCount saved accident scene(s).")
                }
                locations.delete(location)
                locations.flush()
                // Photo blobs are shared by content hash. Retain them so copies keep their pictures.
            }
        } catch (exception: DataIntegrityViolationException) {
            // The foreign key also protects against a scene linking the location during deletion.
            throw ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot delete this location because a saved accident scene uses it.", exception)
        }
    }

    private fun find(id: String): Location = locations.findById(id).orElseThrow {
        ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found: $id")
    }

    override fun photo(id: String): LocationPhoto {
        val key = find(id).photoStorageKey
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "This location has no photo")
        return photos.findById(key).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location photo not found")
        }
    }

    override fun resolve(id: String?, input: LocationInput?, layout: RoadLayoutType?): String? {
        require(id == null || input == null) { "Send locationId or locationInfo, not both" }
        if (id != null) {
            require(id.isNotBlank() && id != "null") { "A valid location ID is required" }
            val location = find(id)
            require(!location.archived) { "This location is archived" }
            require(layout == null || location.roadLayoutType == layout) {
                "The scene road layout must match its selected location"
            }
            return location.id
        }
        return input?.let { save(if (layout == null) it else it.copy(roadLayoutType = layout)).id }
    }

    override fun save(input: LocationInput, photo: MultipartFile?): LocationResponse {
        val normalized = normalize(input)
        val source = normalized.photoSourceLocationId?.let { find(it) }
        require(photo == null || source == null) { "Choose either a new photo or a saved photo" }
        val uploaded = photo?.let { validatePhoto(it) }
        val key = uploaded?.id ?: source?.photoStorageKey
        val fingerprint = fingerprints.fingerprint(normalized, key)

        // The unique fingerprint also protects against simultaneous identical submissions.
        // Retry outside the failed transaction; a caught constraint error cannot be reused.
        repeat(3) { attempt ->
            try {
                return transaction.execute {
                    locations.findByFingerprint(fingerprint)?.let {
                        require(!it.archived) { "The matching location is archived" }
                        return@execute it.toLocationResponse()
                    }
                    // Legacy fingerprints included fileName. Match their remaining fields without
                    // deleting/rekeying old locations or changing historical scene references.
                    locations.findAllByNameAndRoadLayoutTypeAndArchivedFalseOrderByCreatedAtAscIdAsc(
                        normalized.name, normalized.roadLayoutType
                    ).firstOrNull { fingerprints.fingerprint(it) == fingerprint }?.let {
                        return@execute it.toLocationResponse()
                    }
                    if (uploaded != null && !photos.existsById(uploaded.id)) {
                        photos.saveAndFlush(uploaded)
                    }
                    locations.saveAndFlush(Location(
                        name = normalized.name,
                        description = normalized.description, roadLayoutType = normalized.roadLayoutType,
                        backgroundFileName = normalized.backgroundFileName,
                        topRoadWidth = normalized.topRoadWidth, topRoadLanes = normalized.topRoadLanes,
                        bottomRoadWidth = normalized.bottomRoadWidth, bottomRoadLanes = normalized.bottomRoadLanes,
                        leftRoadWidth = normalized.leftRoadWidth, leftRoadLanes = normalized.leftRoadLanes,
                        rightRoadWidth = normalized.rightRoadWidth, rightRoadLanes = normalized.rightRoadLanes,
                        roundaboutDiameter = normalized.roundaboutDiameter, tJunction = normalized.tJunction,
                        photoStorageKey = key,
                        photoOriginalName = photo?.originalFilename?.take(255) ?: source?.photoOriginalName,
                        photoContentType = uploaded?.contentType ?: source?.photoContentType,
                        photoSize = uploaded?.bytes?.size?.toLong() ?: source?.photoSize,
                        fingerprint = fingerprint
                    )).toLocationResponse()
                }!!
            } catch (exception: DataIntegrityViolationException) {
                if (attempt == 2) throw exception
            }
        }
        error("Unable to save location")
    }

    private fun normalize(input: LocationInput): LocationInput {
        fun text(value: String?) = value?.trim()?.takeIf { it.isNotEmpty() }
        require(input.name.trim().isNotEmpty()) { "Location name is required" }
        require(input.name.length <= 255) { "Location name must be at most 255 characters" }
        listOf(input.description, input.backgroundFileName).forEach {
            require(it == null || it.length <= 255) { "Location text fields must be at most 255 characters" }
        }
        listOf(input.topRoadWidth, input.bottomRoadWidth, input.leftRoadWidth,
            input.rightRoadWidth, input.roundaboutDiameter).forEach {
            require(it == null || (it.isFinite() && it > 0)) { "Widths and diameter must be positive finite numbers" }
        }
        listOf(input.topRoadLanes, input.bottomRoadLanes, input.leftRoadLanes, input.rightRoadLanes).forEach {
            require(it == null || it > 0) { "Lane counts must be positive integers" }
        }
        val background = text(input.backgroundFileName)
        require(background == null || background in setOf(
            "glavnaulica.png", "roundabout-3-exits.png", "roundabout-3-exits-2-lanes.png",
            "roundabout-4-way-1-lanes.png", "roundabout-4-way-2-lanes.png", "t-junction.png",
            "boulevard_2_full_lines.png", "boulevard_tree_line.png"
        )) { "Unknown road background" }
        return input.copy(name = input.name.trim(),
            description = text(input.description), backgroundFileName = background,
            photoSourceLocationId = text(input.photoSourceLocationId))
    }

    private fun validatePhoto(file: MultipartFile): LocationPhoto {
        require(!file.isEmpty && file.size <= 10 * 1024 * 1024) { "Photo must be between 1 byte and 10 MB" }
        val bytes = file.bytes
        val stream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
            ?: throw IllegalArgumentException("Invalid photo")
        stream.use {
            val readers = ImageIO.getImageReaders(it)
            require(readers.hasNext()) { "Upload a PNG or JPEG image" }
            val reader = readers.next()
            try {
                reader.input = it
                val format = reader.formatName.lowercase()
                require(format in setOf("png", "jpeg", "jpg")) { "Upload a PNG or JPEG image" }
                require(reader.getWidth(0).toLong() * reader.getHeight(0) <= 25_000_000) {
                    "Photo must not exceed 25 megapixels"
                }
                require(reader.read(0) != null) { "Unable to decode photo" }
                return LocationPhoto(fingerprints.sha256(bytes),
                    if (format == "png") "image/png" else "image/jpeg", bytes)
            } catch (exception: java.io.IOException) {
                throw IllegalArgumentException("The PNG or JPEG photo could not be decoded", exception)
            } finally { reader.dispose() }
        }
    }
}

