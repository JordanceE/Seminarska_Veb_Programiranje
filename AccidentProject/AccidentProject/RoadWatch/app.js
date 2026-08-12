import { createCar } from "./car.js"
import { initCanvas } from "./canvas.js"
import { createMeasurements } from "./measurements.js"
import { createNotes } from "./notes.js"
import { initStorage } from "./storage.js"
import { initAI } from "./ai.js"
import { initUI } from "./ui.js"
import { initBackend } from "./backend-api.js"
import { initSavedScenes } from "./saved-scenes.js"


const canvas =
    document.getElementById("cityCanvas")

const state = {
    canvas,

    ctx:
        canvas.getContext("2d"),

    cars: [],

    selectedCar: null,
    selectedCars: [],
    hoveredCar: null,

    carCounter: 1,

    measurements: [],
    vehicleDistances: {},

    currentMeasurement: null,
    measureMode: false,

    aiMode: false,

    worldScale: 1,

    cameraOffsetX: 0,
    cameraOffsetY: 0,

    backgroundOffsetX: 0,
    backgroundOffsetY: 0,

    actionStack: [],
    redoStack: [],

    backendSceneId: null,

    locationData: {
        scene_type:
            "glavnaulica.png",

        file_name: "",
        name: "",
        desc: "",

        Top: {
            w: null,
            lanes: null
        },

        Bottom: {
            w: null,
            lanes: null
        },

        Right: {
            w: null,
            lanes: null
        },

        Left: {
            w: null,
            lanes: null
        },

        Roundabout: null,
        TJunction: false
    }
}

state.newCar =
    type => createCar(type, state)

const measurements =
    createMeasurements(state)

const notes =
    createNotes(state)

const ui =
    initUI(
        state,
        {
            measurements,
            notes
        }
    )

const canvasView =
    initCanvas(
        state,
        {
            measurements,
            notes,
            ui
        }
    )

const backend =
    initBackend(
        state,
        {
            ui,
            notes,
            canvasView
        }
    )
ui.setBackend(backend)
canvasView.setBackend(backend)
measurements.setBackend(backend)
initStorage(
    state,
    {
        ui,
        notes,
        backend
    }
)

initAI(
    state,
    {
        ui,
        backend
    }
)

initSavedScenes(
    backend
)

canvasView.start()
backend.restoreLastScene()
    .catch(error => {
        console.error(
            "Could not restore the last scene:",
            error
        )
    })