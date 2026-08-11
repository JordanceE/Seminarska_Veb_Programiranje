package com.example.accidentscatchmanagement

import com.example.accidentscatchmanagement.domain.AccidentScene
import com.example.accidentscatchmanagement.domain.commands.AddMeasurementCommand
import com.example.accidentscatchmanagement.domain.commands.AddVehicleCommand
import com.example.accidentscatchmanagement.domain.commands.ArchiveAccidentSceneCommand
import com.example.accidentscatchmanagement.domain.commands.ChangeRoadLayoutCommand
import com.example.accidentscatchmanagement.domain.commands.CreateAccidentSceneCommand
import com.example.accidentscatchmanagement.domain.commands.FinalizeAccidentSceneCommand
import com.example.accidentscatchmanagement.domain.commands.RemoveMeasurementCommand
import com.example.accidentscatchmanagement.domain.commands.RemoveVehicleCommand
import com.example.accidentscatchmanagement.domain.commands.StoreAIAnalysisCommand
import com.example.accidentscatchmanagement.domain.commands.StoreFullSceneCommand
import com.example.accidentscatchmanagement.domain.commands.UpdateSceneLocationCommand
import com.example.accidentscatchmanagement.domain.commands.UpdateVehicleCommand
import com.example.accidentscatchmanagement.domain.enums.MeasurementType
import com.example.accidentscatchmanagement.domain.enums.RoadLayoutType
import com.example.accidentscatchmanagement.domain.enums.SceneStatus
import com.example.accidentscatchmanagement.domain.enums.VehicleType
import com.example.accidentscatchmanagement.domain.events.AIAnalysisStoredEvent
import com.example.accidentscatchmanagement.domain.events.AccidentSceneArchivedEvent
import com.example.accidentscatchmanagement.domain.events.AccidentSceneCreatedEvent
import com.example.accidentscatchmanagement.domain.events.AccidentSceneFinalizedEvent
import com.example.accidentscatchmanagement.domain.events.MeasurementAddedEvent
import com.example.accidentscatchmanagement.domain.events.VehicleAddedEvent
import com.example.accidentscatchmanagement.domain.valueobjects.CanvasPosition
import com.example.accidentscatchmanagement.domain.valueobjects.LocationInfo
import com.example.accidentscatchmanagement.domain.valueobjects.MeasurementLine
import com.example.accidentscatchmanagement.domain.valueobjects.VehiclePlacement
import org.axonframework.test.aggregate.AggregateTestFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AccidentScatchManagementApplicationTests {

    private lateinit var fixture: AggregateTestFixture<AccidentScene>

    private val sceneId = "AccidentScene:test-1"

    private val location = LocationInfo(
        fileName = "scene-001.json",
        name = "Main street accident",
        description = "Test accident scene",
        topRoadWidth = 7.0,
        topRoadLanes = 2,
        bottomRoadWidth = 7.0,
        bottomRoadLanes = 2,
        leftRoadWidth = 6.0,
        leftRoadLanes = 1,
        rightRoadWidth = 6.0,
        rightRoadLanes = 1
    )

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(AccidentScene::class.java)
    }

    // -------------------------------------------------------------------------
    // Accident-scene creation
    // -------------------------------------------------------------------------

    @Test
    fun `create scene initializes draft aggregate`() {
        fixture.givenNoPriorActivity()
            .`when`(
                CreateAccidentSceneCommand(
                    accidentSceneId = sceneId,
                    roadLayoutType = RoadLayoutType.INTERSECTION,
                    locationInfo = location
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.id == sceneId)
                check(scene.roadLayoutType == RoadLayoutType.INTERSECTION)
                check(scene.locationInfo == location)
                check(scene.status == SceneStatus.DRAFT)
                check(scene.vehicles.isEmpty())
                check(scene.measurements.isEmpty())
            }
    }

    // -------------------------------------------------------------------------
    // Road-layout tests
    // -------------------------------------------------------------------------

    @Test
    fun `change road layout updates scene`() {
        fixture.given(createdEvent())
            .`when`(
                ChangeRoadLayoutCommand(
                    accidentSceneId = sceneId,
                    roadLayoutType = RoadLayoutType.T_JUNCTION
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.roadLayoutType == RoadLayoutType.T_JUNCTION)
                check(scene.status == SceneStatus.DRAFT)
            }
    }

    // -------------------------------------------------------------------------
    // Location tests
    // -------------------------------------------------------------------------

    @Test
    fun `update location replaces location information`() {
        val updatedLocation = location.copy(
            name = "Updated scene",
            description = "Updated description"
        )

        fixture.given(createdEvent())
            .`when`(
                UpdateSceneLocationCommand(
                    accidentSceneId = sceneId,
                    locationInfo = updatedLocation
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.locationInfo == updatedLocation)
            }
    }

    // -------------------------------------------------------------------------
    // Vehicle tests
    // -------------------------------------------------------------------------

    @Test
    fun `add vehicle stores vehicle`() {
        val vehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        fixture.given(createdEvent())
            .`when`(
                AddVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicle = vehicle
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.vehicles.size == 1)
                check(scene.vehicles.first() == vehicle)
            }
    }

    @Test
    fun `add multiple vehicles stores all vehicles`() {
        val firstVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        val secondVehicle = vehicle(
            id = "Vehicle:2",
            name = "V2"
        )

        fixture.given(
            createdEvent(),
            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = firstVehicle
            )
        )
            .`when`(
                AddVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicle = secondVehicle
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.vehicles.size == 2)
                check(scene.vehicles[0] == firstVehicle)
                check(scene.vehicles[1] == secondVehicle)
            }
    }

    @Test
    fun `add duplicate vehicle is rejected`() {
        val vehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        fixture.given(
            createdEvent(),
            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = vehicle
            )
        )
            .`when`(
                AddVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicle = vehicle.copy()
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Vehicle with id Vehicle:1 already exists"
            )
    }

    @Test
    fun `add vehicle with blank name is rejected`() {
        val invalidVehicle = vehicle(
            id = "Vehicle:1",
            name = ""
        )

        fixture.given(createdEvent())
            .`when`(
                AddVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicle = invalidVehicle
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Vehicle name cannot be blank"
            )
    }

    @Test
    fun `add vehicle with invalid width is rejected`() {
        val invalidVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        ).copy(
            width = 0.0
        )

        fixture.given(createdEvent())
            .`when`(
                AddVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicle = invalidVehicle
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Vehicle width must be positive"
            )
    }

    @Test
    fun `add vehicle with invalid height is rejected`() {
        val invalidVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        ).copy(
            height = 0.0
        )

        fixture.given(createdEvent())
            .`when`(
                AddVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicle = invalidVehicle
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Vehicle height must be positive"
            )
    }

    @Test
    fun `add vehicle with invalid scale is rejected`() {
        val invalidVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        ).copy(
            scale = 0.0
        )

        fixture.given(createdEvent())
            .`when`(
                AddVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicle = invalidVehicle
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Vehicle scale must be positive"
            )
    }

    @Test
    fun `update existing vehicle replaces its data`() {
        val original = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        val updated = original.copy(
            name = "Updated V1",
            color = "Blue",
            position = CanvasPosition(
                x = 300.0,
                y = 150.0
            )
        )

        fixture.given(
            createdEvent(),
            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = original
            )
        )
            .`when`(
                UpdateVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicleId = original.vehicleId,
                    vehicle = updated
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.vehicles.size == 1)
                check(scene.vehicles.first() == updated)
                check(scene.vehicles.first().name == "Updated V1")
                check(scene.vehicles.first().color == "Blue")
                check(scene.vehicles.first().position.x == 300.0)
                check(scene.vehicles.first().position.y == 150.0)
            }
    }

    @Test
    fun `update missing vehicle is rejected`() {
        fixture.given(createdEvent())
            .`when`(
                UpdateVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicleId = "Vehicle:missing",
                    vehicle = vehicle(
                        id = "Vehicle:missing",
                        name = "Missing"
                    )
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Vehicle with id Vehicle:missing does not exist"
            )
    }

    @Test
    fun `remove vehicle deletes vehicle`() {
        val vehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        fixture.given(
            createdEvent(),
            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = vehicle
            )
        )
            .`when`(
                RemoveVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicleId = vehicle.vehicleId
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.vehicles.isEmpty())
            }
    }

    @Test
    fun `remove vehicle also removes measurements originating from it`() {
        val firstVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        val secondVehicle = vehicle(
            id = "Vehicle:2",
            name = "V2"
        )

        val measurement = measurement(
            id = "Measurement:1",
            fromVehicleId = firstVehicle.vehicleId
        )

        fixture.given(
            createdEvent(),

            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = firstVehicle
            ),

            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = secondVehicle
            ),

            MeasurementAddedEvent(
                accidentSceneId = sceneId,
                measurement = measurement
            )
        )
            .`when`(
                RemoveVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicleId = firstVehicle.vehicleId
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(
                    scene.vehicles.map { it.vehicleId } ==
                            listOf(secondVehicle.vehicleId)
                )

                check(scene.measurements.isEmpty())
            }
    }

    @Test
    fun `remove missing vehicle is rejected`() {
        fixture.given(createdEvent())
            .`when`(
                RemoveVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicleId = "Vehicle:missing"
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Vehicle with id Vehicle:missing does not exist"
            )
    }

    // -------------------------------------------------------------------------
    // Measurement tests
    // -------------------------------------------------------------------------

    @Test
    fun `add measurement stores valid measurement`() {
        val vehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        val measurement = measurement(
            id = "Measurement:1",
            fromVehicleId = vehicle.vehicleId
        )

        fixture.given(
            createdEvent(),

            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = vehicle
            )
        )
            .`when`(
                AddMeasurementCommand(
                    accidentSceneId = sceneId,
                    measurement = measurement
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.measurements.size == 1)
                check(scene.measurements.first() == measurement)
            }
    }

    @Test
    fun `add measurement without vehicle reference is accepted`() {
        val measurement = measurement(
            id = "Measurement:1",
            fromVehicleId = null
        )

        fixture.given(createdEvent())
            .`when`(
                AddMeasurementCommand(
                    accidentSceneId = sceneId,
                    measurement = measurement
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.measurements.size == 1)
                check(scene.measurements.first() == measurement)
            }
    }

    @Test
    fun `add measurement referencing missing source vehicle is rejected`() {
        val measurement = measurement(
            id = "Measurement:1",
            fromVehicleId = "Vehicle:missing"
        )

        fixture.given(createdEvent())
            .`when`(
                AddMeasurementCommand(
                    accidentSceneId = sceneId,
                    measurement = measurement
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Measurement references a source vehicle that does not exist"
            )
    }

    @Test
    fun `remove measurement deletes it`() {
        val measurement = measurement(
            id = "Measurement:1"
        )

        fixture.given(
            createdEvent(),

            MeasurementAddedEvent(
                accidentSceneId = sceneId,
                measurement = measurement
            )
        )
            .`when`(
                RemoveMeasurementCommand(
                    accidentSceneId = sceneId,
                    measurementId = measurement.measurementId
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.measurements.isEmpty())
            }
    }

    @Test
    fun `remove missing measurement is rejected`() {
        fixture.given(createdEvent())
            .`when`(
                RemoveMeasurementCommand(
                    accidentSceneId = sceneId,
                    measurementId = "Measurement:missing"
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Measurement with id Measurement:missing does not exist"
            )
    }

    // -------------------------------------------------------------------------
    // Full-scene storage tests
    // -------------------------------------------------------------------------

    @Test
    fun `store full scene replaces layout location vehicles and measurements`() {
        val vehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        val measurement = measurement(
            id = "Measurement:1",
            fromVehicleId = vehicle.vehicleId
        )

        val newLocation = location.copy(
            name = "Roundabout scene"
        )

        fixture.given(createdEvent())
            .`when`(
                StoreFullSceneCommand(
                    accidentSceneId = sceneId,
                    roadLayoutType =
                        RoadLayoutType.ROUNDABOUT_4_EXITS_2_LANES,
                    locationInfo = newLocation,
                    vehicles = listOf(vehicle),
                    measurements = listOf(measurement)
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(
                    scene.roadLayoutType ==
                            RoadLayoutType.ROUNDABOUT_4_EXITS_2_LANES
                )

                check(scene.locationInfo == newLocation)
                check(scene.vehicles == listOf(vehicle))
                check(scene.measurements == listOf(measurement))
            }
    }

    @Test
    fun `store full scene can clear vehicles and measurements`() {
        val existingVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        fixture.given(
            createdEvent(),

            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = existingVehicle
            )
        )
            .`when`(
                StoreFullSceneCommand(
                    accidentSceneId = sceneId,
                    roadLayoutType = RoadLayoutType.INTERSECTION,
                    locationInfo = location,
                    vehicles = emptyList(),
                    measurements = emptyList()
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.vehicles.isEmpty())
                check(scene.measurements.isEmpty())
            }
    }

    @Test
    fun `store full scene validates every vehicle`() {
        val invalidVehicle = vehicle(
            id = "Vehicle:1",
            name = ""
        )

        fixture.given(createdEvent())
            .`when`(
                StoreFullSceneCommand(
                    accidentSceneId = sceneId,
                    roadLayoutType = RoadLayoutType.INTERSECTION,
                    locationInfo = location,
                    vehicles = listOf(invalidVehicle),
                    measurements = emptyList()
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Vehicle name cannot be blank"
            )
    }

    // -------------------------------------------------------------------------
    // AI-analysis tests
    // -------------------------------------------------------------------------

    @Test
    fun `store AI analysis replaces vehicles and updates AI fields`() {
        val manualVehicle = vehicle(
            id = "Vehicle:manual",
            name = "Manual"
        )

        val detectedVehicle = vehicle(
            id = "Vehicle:ai-1",
            name = "AI V1"
        )

        fixture.given(
            createdEvent(),

            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = manualVehicle
            )
        )
            .`when`(
                StoreAIAnalysisCommand(
                    accidentSceneId = sceneId,
                    detectedVehicles = listOf(detectedVehicle),
                    confidence = 0.92,
                    summary = "Detected one vehicle"
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.vehicles == listOf(detectedVehicle))
                check(scene.aiConfidence == 0.92)
                check(scene.aiSummary == "Detected one vehicle")
                check(scene.status == SceneStatus.AI_ANALYZED)
            }
    }

    @Test
    fun `AI analyzed scene remains editable`() {
        val detectedVehicle = vehicle(
            id = "Vehicle:ai-1",
            name = "AI V1"
        )

        val additionalVehicle = vehicle(
            id = "Vehicle:2",
            name = "V2"
        )

        fixture.given(
            createdEvent(),

            AIAnalysisStoredEvent(
                accidentSceneId = sceneId,
                detectedVehicles = listOf(detectedVehicle),
                confidence = 0.8,
                summary = "AI result",
                measurements = emptyList()
            )
        )
            .`when`(
                AddVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicle = additionalVehicle
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.status == SceneStatus.AI_ANALYZED)
                check(scene.vehicles.size == 2)
            }
    }

    // -------------------------------------------------------------------------
    // Finalization tests
    // -------------------------------------------------------------------------

    @Test
    fun `finalize scene with vehicles changes status`() {
        val vehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        fixture.given(
            createdEvent(),

            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = vehicle
            )
        )
            .`when`(
                FinalizeAccidentSceneCommand(
                    accidentSceneId = sceneId
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.status == SceneStatus.FINALIZED)
            }
    }

    @Test
    fun `finalize empty scene is rejected`() {
        fixture.given(createdEvent())
            .`when`(
                FinalizeAccidentSceneCommand(
                    accidentSceneId = sceneId
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Cannot finalize an accident scene without vehicles"
            )
    }

    @Test
    fun `finalized scene cannot be modified`() {
        val vehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        fixture.given(
            createdEvent(),

            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = vehicle
            ),

            AccidentSceneFinalizedEvent(
                accidentSceneId = sceneId
            )
        )
            .`when`(
                ChangeRoadLayoutCommand(
                    accidentSceneId = sceneId,
                    roadLayoutType =
                        RoadLayoutType.BOULEVARD_TREE_MEDIAN
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Scene cannot be modified when status is FINALIZED"
            )
    }

    // -------------------------------------------------------------------------
    // Archive tests
    // -------------------------------------------------------------------------

    @Test
    fun `archive finalized scene changes status`() {
        val vehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        fixture.given(
            createdEvent(),

            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = vehicle
            ),

            AccidentSceneFinalizedEvent(
                accidentSceneId = sceneId
            )
        )
            .`when`(
                ArchiveAccidentSceneCommand(
                    accidentSceneId = sceneId
                )
            )
            .expectSuccessfulHandlerExecution()
            .expectState { scene ->
                check(scene.status == SceneStatus.ARCHIVED)
            }
    }

    @Test
    fun `archive draft scene is rejected`() {
        fixture.given(createdEvent())
            .`when`(
                ArchiveAccidentSceneCommand(
                    accidentSceneId = sceneId
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Only finalized scenes can be archived"
            )
    }

    @Test
    fun `archived scene cannot be modified`() {
        val firstVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        val secondVehicle = vehicle(
            id = "Vehicle:2",
            name = "V2"
        )

        fixture.given(
            createdEvent(),

            VehicleAddedEvent(
                accidentSceneId = sceneId,
                vehicle = firstVehicle
            ),

            AccidentSceneFinalizedEvent(
                accidentSceneId = sceneId
            ),

            AccidentSceneArchivedEvent(
                accidentSceneId = sceneId
            )
        )
            .`when`(
                AddVehicleCommand(
                    accidentSceneId = sceneId,
                    vehicle = secondVehicle
                )
            )
            .expectException(IllegalArgumentException::class.java)
            .expectExceptionMessage(
                "Scene cannot be modified when status is ARCHIVED"
            )
    }

    // -------------------------------------------------------------------------
    // Value-object validation tests
    // -------------------------------------------------------------------------

    @Test
    fun `negative measurement length is rejected by value object`() {
        val exception = assertThrows<IllegalArgumentException> {
            MeasurementLine(
                measurementId = "Measurement:invalid",
                lengthMeters = -1.0
            )
        }

        check(
            exception.message ==
                    "Measurement length cannot be negative"
        )
    }

    @Test
    fun `vehicle validation rejects blank vehicle name`() {
        val invalidVehicle = vehicle(
            id = "Vehicle:1",
            name = ""
        )

        val exception = assertThrows<IllegalArgumentException> {
            invalidVehicle.validate()
        }

        check(
            exception.message ==
                    "Vehicle name cannot be blank"
        )
    }

    @Test
    fun `vehicle validation rejects non-positive width`() {
        val invalidVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        ).copy(
            width = 0.0
        )

        val exception = assertThrows<IllegalArgumentException> {
            invalidVehicle.validate()
        }

        check(
            exception.message ==
                    "Vehicle width must be positive"
        )
    }

    @Test
    fun `vehicle validation rejects non-positive height`() {
        val invalidVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        ).copy(
            height = -10.0
        )

        val exception = assertThrows<IllegalArgumentException> {
            invalidVehicle.validate()
        }

        check(
            exception.message ==
                    "Vehicle height must be positive"
        )
    }

    @Test
    fun `vehicle validation rejects non-positive scale`() {
        val invalidVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        ).copy(
            scale = 0.0
        )

        val exception = assertThrows<IllegalArgumentException> {
            invalidVehicle.validate()
        }

        check(
            exception.message ==
                    "Vehicle scale must be positive"
        )
    }

    @Test
    fun `valid vehicle passes validation`() {
        val validVehicle = vehicle(
            id = "Vehicle:1",
            name = "V1"
        )

        validVehicle.validate()
    }

    // -------------------------------------------------------------------------
    // Helper functions
    // -------------------------------------------------------------------------

    private fun createdEvent(): AccidentSceneCreatedEvent {
        return AccidentSceneCreatedEvent(
            accidentSceneId = sceneId,
            roadLayoutType = RoadLayoutType.INTERSECTION,
            locationInfo = location
        )
    }

    private fun vehicle(
        id: String,
        name: String
    ): VehiclePlacement {
        return VehiclePlacement(
            vehicleId = id,
            name = name,
            type = VehicleType.SEDAN,
            model = "Test model",
            color = "Red",
            plate = "TEST-001",
            guilty = false,
            comment = "Test vehicle",
            position = CanvasPosition(
                x = 100.0,
                y = 200.0
            ),
            width = 90.0,
            height = 50.0,
            rotation = 0.0,
            scale = 1.0,
            flipped = false,
            note = "Test note"
        )
    }

    private fun measurement(
        id: String,
        fromVehicleId: String? = null
    ): MeasurementLine {
        return MeasurementLine(
            measurementId = id,
            fromVehicleId = fromVehicleId,
            type = MeasurementType.VEHICLE_TO_POINT,
            x1 = 100.0,
            y1 = 200.0,
            x2 = 200.0,
            y2 = 250.0,
            lengthMeters = 5.5,
            label = "Test measurement"
        )
    }
}
