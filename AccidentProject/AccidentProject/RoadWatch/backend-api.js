const API_BASE =
    "http://localhost:8080/api/accident-scenes"

const ROAD_LAYOUT_TO_BACKEND = {
    "glavnaulica.png":
        "INTERSECTION",

    "roundabout-3-exits.png":
        "ROUNDABOUT_3_EXITS_1_LANE",

    "roundabout-3-exits-2-lanes.png":
        "ROUNDABOUT_3_EXITS_2_LANES",

    "roundabout-4-way-1-lanes.png":
        "ROUNDABOUT_4_EXITS_1_LANE",

    "roundabout-4-way-2-lanes.png":
        "ROUNDABOUT_4_EXITS_2_LANES",

    "t-junction.png":
        "T_JUNCTION",

    "boulevard_2_full_lines.png":
        "BOULEVARD_SOLID_MEDIAN",

    "boulevard_tree_line.png":
        "BOULEVARD_TREE_MEDIAN"
}

const ROAD_LAYOUT_TO_FRONTEND = Object.fromEntries(
    Object.entries(ROAD_LAYOUT_TO_BACKEND)
        .map(([frontend, backend]) => [
            backend,
            frontend
        ])
)

const VEHICLE_TYPE_TO_BACKEND = {
    hatchback: "HATCHBACK",
    sedan: "SEDAN",
    coupe: "COUPE",
    wagon: "WAGON",
    suv: "SUV",
    mpv: "MINIVAN",
    minivan: "MINIVAN",
    pickup: "PICKUP",
    van: "VAN",
    truck: "TRUCK",
    motorcycle: "MOTORCYCLE",
    bus: "BUS"
}

const VEHICLE_TYPE_TO_FRONTEND = {
    HATCHBACK: "hatchback",
    SEDAN: "sedan",
    COUPE: "coupe",
    WAGON: "wagon",
    SUV: "suv",
    MINIVAN: "mpv",
    PICKUP: "pickup",
    VAN: "van",
    TRUCK: "truck",
    MOTORCYCLE: "motorcycle",
    BUS: "bus"
}

function createId(prefix) {
    const value = crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random()
            .toString(16)
            .slice(2)}`

    return `${prefix}:${value}`
}

async function request(url, options = {}) {
    const response = await fetch(url, options)

    const text = await response.text()

    let body = null

    if (text) {
        try {
            body = JSON.parse(text)
        } catch {
            body = text
        }
    }

    if (!response.ok) {
        const message =
            body?.message ||
            body?.error ||
            body ||
            `Request failed with status ${response.status}`

        throw new Error(message)
    }

    return body
}

function toNullableNumber(value) {
    if (
        value === "" ||
        value === null ||
        value === undefined
    ) {
        return null
    }

    const number = Number(value)

    return Number.isFinite(number)
        ? number
        : null
}

function toBackendLocation(location) {
    return {
        fileName:
            location.file_name || null,

        name:
            location.name || null,

        description:
            location.desc || null,

        topRoadWidth:
            toNullableNumber(location.Top?.w),

        topRoadLanes:
            toNullableNumber(location.Top?.lanes),

        bottomRoadWidth:
            toNullableNumber(location.Bottom?.w),

        bottomRoadLanes:
            toNullableNumber(location.Bottom?.lanes),

        leftRoadWidth:
            toNullableNumber(location.Left?.w),

        leftRoadLanes:
            toNullableNumber(location.Left?.lanes),

        rightRoadWidth:
            toNullableNumber(location.Right?.w),

        rightRoadLanes:
            toNullableNumber(location.Right?.lanes),

        roundaboutDiameter:
            toNullableNumber(location.Roundabout),

        tJunction:
            Boolean(location.TJunction)
    }
}

function fromBackendLocation(location, roadLayoutType) {
    return {
        scene_type:
            ROAD_LAYOUT_TO_FRONTEND[roadLayoutType] ||
            "glavnaulica.png",

        file_name:
            location?.fileName || "",

        name:
            location?.name || "",

        desc:
            location?.description || "",

        Top: {
            w: location?.topRoadWidth ?? null,
            lanes: location?.topRoadLanes ?? null
        },

        Bottom: {
            w: location?.bottomRoadWidth ?? null,
            lanes: location?.bottomRoadLanes ?? null
        },

        Left: {
            w: location?.leftRoadWidth ?? null,
            lanes: location?.leftRoadLanes ?? null
        },

        Right: {
            w: location?.rightRoadWidth ?? null,
            lanes: location?.rightRoadLanes ?? null
        },

        Roundabout:
            location?.roundaboutDiameter ?? null,

        TJunction:
            Boolean(location?.tJunction)
    }
}

function ensureVehicleId(car) {
    if (!car.vehicleId) {
        car.vehicleId = createId("Vehicle")
    }

    return car.vehicleId
}

function toBackendVehicle(car) {
    return {
        vehicleId:
            ensureVehicleId(car),

        name:
            car.vehicleData?.name || "Vehicle",

        type:
            VEHICLE_TYPE_TO_BACKEND[
                car.vehicleData?.type
            ] || "SEDAN",

        model:
            car.vehicleData?.model || null,

        color:
            car.vehicleData?.color || null,

        plate:
            car.vehicleData?.plate || null,

        guilty:
            Boolean(car.vehicleData?.guilty),

        comment:
            car.vehicleData?.comment || null,

        position: {
            x: Number(car.x) || 0,
            y: Number(car.y) || 0
        },

        width:
            Number(car.width) || 60,

        height:
            Number(car.height) || 20,

        rotation:
            Number(car.rotation) || 0,

        scale:
            Number(car.scale) || 1,

        flipped:
            Boolean(car.flipped),

        note:
            car.note || null,

        confidence:
            car.confidence ?? null
    }
}

function ensureMeasurementId(measurement) {
    if (!measurement.measurementId) {
        measurement.measurementId =
            createId("Measurement")
    }

    return measurement.measurementId
}

function resolveVehicleId(state, measurement) {
    if (measurement.fromVehicleId) {
        return measurement.fromVehicleId
    }

    if (!measurement.from) {
        return null
    }

    return state.cars.find(
        car =>
            car.vehicleData?.name ===
            measurement.from
    )?.vehicleId || null
}

function toBackendMeasurement(state, measurement) {
    const fromVehicleId =
        resolveVehicleId(state, measurement)

    return {
        measurementId:
            ensureMeasurementId(measurement),

        fromVehicleId,

        toVehicleId:
            measurement.toVehicleId || null,

        type:
            measurement.type ||
            (
                fromVehicleId
                    ? "VEHICLE_TO_POINT"
                    : "POINT_TO_POINT"
            ),

        x1:
            Number(measurement.x1) || 0,

        y1:
            Number(measurement.y1) || 0,

        x2:
            Number(measurement.x2) || 0,

        y2:
            Number(measurement.y2) || 0,

        lengthMeters:
            Number(measurement.len) || 0,

        label:
            measurement.label ||
            measurement.from ||
            null
    }
}

function fromBackendVehicle(state, vehicle) {
    const frontendType =
        VEHICLE_TYPE_TO_FRONTEND[vehicle.type] ||
        "sedan"

    const car = state.newCar(frontendType)

    car.vehicleId =
        vehicle.vehicleId

    car.x =
        vehicle.position?.x ?? 0

    car.y =
        vehicle.position?.y ?? 0

    car.width =
        vehicle.width ?? 60

    car.height =
        vehicle.height ?? 20

    car.rotation =
        vehicle.rotation ?? 0

    car.scale =
        vehicle.scale ?? 1

    car.flipped =
        Boolean(vehicle.flipped)

    car.note =
        vehicle.note || ""

    car.confidence = vehicle.confidence ?? null;

    car.vehicleData = {
        name:
            vehicle.name || "Vehicle",

        model:
            vehicle.model || "",

        type:
            frontendType,

        color:
            vehicle.color || "",

        plate:
            vehicle.plate || "",

        guilty:
            Boolean(vehicle.guilty),

        comment:
            vehicle.comment || ""
    }

    return car
}

export function initBackend(
    state,
    {
        ui,
        notes,
        canvasView
    }
) {
    const rememberedSceneId =
    localStorage.getItem(
        "currentAccidentSceneId"
    )
    state.backendSceneId = null

    function updateSceneIdDisplay() {
        const element =
            document.getElementById(
                "currentBackendScene"
            )

        if (!element) return

        element.textContent =
            state.backendSceneId ||
            "Not saved yet"
    }

    function storeCurrentId(id) {
        state.backendSceneId = id

        localStorage.setItem(
            "currentAccidentSceneId",
            id
        )

        updateSceneIdDisplay()
    }

    function clearCurrentId() {
        state.backendSceneId = null

        localStorage.removeItem(
            "currentAccidentSceneId"
        )

        updateSceneIdDisplay()
    }

    function currentCreateRequest() {
        return {
            roadLayoutType:
                ROAD_LAYOUT_TO_BACKEND[
                    state.locationData.scene_type
                ] || "INTERSECTION",

            locationInfo:
                toBackendLocation(
                    state.locationData
                )
        }
    }

    function currentFullSceneRequest() {
        return {
            ...currentCreateRequest(),

            vehicles:
                state.cars.map(
                    toBackendVehicle
                ),

            measurements:
                state.measurements.map(
                    measurement =>
                        toBackendMeasurement(
                            state,
                            measurement
                        )
                )
        }
    }

    async function createScene() {
        const result = await request(
            API_BASE,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/json"
                },

                body: JSON.stringify(
                    currentCreateRequest()
                )
            }
        )

        storeCurrentId(result.id)

        return result.id
    }

    async function saveCurrentScene() {
        const id =
            state.backendSceneId ||
            await createScene()

        await request(
            `${API_BASE}/${encodeURIComponent(id)}/full-scene`,
            {
                method: "PUT",

                headers: {
                    "Content-Type":
                        "application/json"
                },

                body: JSON.stringify(
                    currentFullSceneRequest()
                )
            }
        )

        storeCurrentId(id)

        return id
    }

    async function saveAsNewScene() {
        clearCurrentId()

        return saveCurrentScene()
    }
    async function createNewScene() {
    /*
     * The frontend state must be cleared before this
     * function is called. createScene() reads the
     * current frontend location and road layout.
     */
    clearCurrentId()

    return createScene()
}
    async function getScene(id) {
        return request(
            `${API_BASE}/${encodeURIComponent(id)}`
        )
    }
    async function deleteScene(id) {
    if (!id) {
        throw new Error("An accident scene ID is required.");
    }

    await request(
        `${API_BASE}/${encodeURIComponent(id)}`,
        {
            method: "DELETE"
        }
    );

    /*
     * Clear the currently loaded database ID when
     * the scene being deleted is open in the editor.
     */
    if (state.backendSceneId === id) {
        clearCurrentId();
    }
}

    function applyScene(scene) {
         state.aiConfidence =
        scene.aiConfidence ?? null;

    state.aiSummary =
        scene.aiSummary ?? null;

        const cars = scene.vehicles.map(
            vehicle =>
                fromBackendVehicle(
                    state,
                    vehicle
                )
        )

        const carsById = new Map(
            cars.map(car => [
                car.vehicleId,
                car
            ])
        )

        const loadedMeasurements =
             (scene.measurements || []).map(
                measurement => ({
                    measurementId:
                        measurement.measurementId,

                    fromVehicleId:
                        measurement.fromVehicleId,

                    toVehicleId:
                        measurement.toVehicleId,

                    type:
                        measurement.type,

                    from:
                        carsById
                            .get(
                                measurement
                                    .fromVehicleId
                            )
                            ?.vehicleData
                            ?.name || null,

                    x1:
                        measurement.x1,

                    y1:
                        measurement.y1,

                    x2:
                        measurement.x2,

                    y2:
                        measurement.y2,

                    len:
                        measurement.lengthMeters,

                    label:
                        measurement.label,

                    manual:
                        true
                })
            )

        state.locationData =
            fromBackendLocation(
                scene.locationInfo,
                scene.roadLayoutType
            )

        state.cars =
            cars

        state.measurements =
            loadedMeasurements

        state.vehicleDistances = {}

        state.selectedCar = null
        state.selectedCars = []

        state.carCounter =
            cars.length + 1

        storeCurrentId(scene.id)

        ui.loadLocation()
        ui.refresh()
        notes.updateList()

        canvasView.loadBackground(
            state.locationData.scene_type
        )
    }

    async function loadScene(id) {
        const scene =
            await getScene(id)

        applyScene(scene)

        return scene
    }
    async function restoreLastScene() {
    if (!rememberedSceneId) {
        return null
    }

    try {
        return await loadScene(
            rememberedSceneId
        )
    } catch (error) {
        clearCurrentId()
        throw error
    }
}

    async function searchScenes({
        query = "",
        status = "",
        page = 0,
        size = 12
    } = {}) {
        const params =
            new URLSearchParams()

        params.set("page", String(page))
        params.set("size", String(size))
        params.append(
            "sort",
            "updatedAt,desc"
        )

        if (query.trim()) {
            params.set(
                "query",
                query.trim()
            )
        }

        if (status) {
            params.set(
                "status",
                status
            )
        }
         const url =
        `${API_BASE}?${params.toString()}`;

    console.log(
        "[Backend API] GET",
        url
    );

        return request(url)
    }
    async function ensureCurrentScene() {
    if (state.backendSceneId) {
        return state.backendSceneId
    }

    return createScene()
}

async function addVehicle(car) {
    const sceneId =
        await ensureCurrentScene()

    await request(
        `${API_BASE}/${
            encodeURIComponent(sceneId)
        }/vehicles`,
        {
            method: "POST",
            headers: {
                "Content-Type":
                    "application/json"
            },
            body: JSON.stringify({
                vehicle:
                    toBackendVehicle(car)
            })
        }
    )

    return sceneId
}

async function removeVehicle(vehicleId) {
    if (!state.backendSceneId) {
        return
    }

    await request(
        `${API_BASE}/${
            encodeURIComponent(
                state.backendSceneId
            )
        }/vehicles/${
            encodeURIComponent(vehicleId)
        }`,
        {
            method: "DELETE"
        }
    )
}

async function addMeasurement(measurement) {
    const sceneId =
        await ensureCurrentScene()

    await request(
        `${API_BASE}/${
            encodeURIComponent(sceneId)
        }/measurements`,
        {
            method: "POST",
            headers: {
                "Content-Type":
                    "application/json"
            },
            body: JSON.stringify({
                measurement:
                    toBackendMeasurement(
                        state,
                        measurement
                    )
            })
        }
    )

    return sceneId
}

async function removeMeasurement(
    measurementId
) {
    if (!state.backendSceneId) {
        return
    }

    await request(
        `${API_BASE}/${
            encodeURIComponent(
                state.backendSceneId
            )
        }/measurements/${
            encodeURIComponent(
                measurementId
            )
        }`,
        {
            method: "DELETE"
        }
    )
}

async function finalizeScene(sceneId) {
    if (!sceneId) {
        throw new Error(
            "An accident scene ID is required."
        )
    }

    await request(
        `${API_BASE}/${
            encodeURIComponent(sceneId)
        }/finalize`,
        {
            method: "POST"
        }
    )
}

async function archiveScene(sceneId) {
    if (!sceneId) {
        throw new Error(
            "An accident scene ID is required."
        )
    }

    await request(
        `${API_BASE}/${
            encodeURIComponent(sceneId)
        }/archive`,
        {
            method: "POST"
        }
    )
}
    async function analyzeImage(file) {
        const id =
            await saveCurrentScene()

        const form =
            new FormData()

        form.append(
            "image",
            file
        )

        const analysis =
            await request(
                `${API_BASE}/${encodeURIComponent(id)}/analyze`,
                {
                    method: "POST",
                    body: form
                }
            )

        await loadScene(id)

        return analysis
    }

    updateSceneIdDisplay()

    return {
    saveCurrentScene,
    saveAsNewScene,
    loadScene,
    restoreLastScene,
    getScene,
    searchScenes,
    analyzeImage,
    deleteScene,
    addVehicle,
    removeVehicle,
    addMeasurement,
    removeMeasurement,
    finalizeScene,
    archiveScene,
        createNewScene
}
}