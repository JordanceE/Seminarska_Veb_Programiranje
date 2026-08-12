import { SCALE_REAL } from "./car.js"

const $ = id => document.getElementById(id)
export function initUI(state, { measurements, notes }) {
    let backend = null

function setBackend(value) {
    backend = value
}
async function resetScene() {
    if (!backend) {
        alert(
            "The backend connection is not ready."
        )
        return
    }

    const confirmed =
        window.confirm(
            "Save the current scene and create " +
            "a new empty scene?"
        )

    if (!confirmed) {
        return
    }

    const resetButton =
        $("resetBtn")

    const originalText =
        resetButton.textContent

    let savedSceneId = null

    resetButton.disabled = true
    resetButton.textContent =
        "Creating New Scene..."

    try {
        /*
         * Save the complete current scene before
         * clearing anything.
         */
        savedSceneId =
            await backend.saveCurrentScene()

        /*
         * Preserve only the selected road background.
         */
        const currentRoadLayout =
            state.locationData.scene_type ||
            "glavnaulica.png"

        state.cars = []
        state.measurements = []
        state.vehicleDistances = {}

        state.selectedCar = null
        state.selectedCars = []
        state.hoveredCar = null

        state.currentMeasurement = null
        state.measureMode = false
        state.aiMode = false

        state.aiConfidence = null
        state.aiSummary = null

        state.carCounter = 1

        state.worldScale = 1
        state.cameraOffsetX = 0
        state.cameraOffsetY = 0
        state.backgroundOffsetX = 0
        state.backgroundOffsetY = 0

        /*
         * Do not allow Undo to bring vehicles from
         * the previous database scene into the new one.
         */
        state.actionStack = []
        state.redoStack = []

        /*
         * Create blank location information while
         * preserving the selected road background.
         */
        state.locationData = {
            scene_type:
                currentRoadLayout,

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

        $("measureBtn").classList.remove(
            "active"
        )

        const editModeButton =
            $("editModeBtn")

        if (editModeButton) {
            editModeButton.textContent =
                "Switch to AI Mode"
        }

        loadLocation()
        refresh()
        updateZoom()
        notes.updateList()
        measurements.updateList()

        /*
         * createScene() now reads the cleared state,
         * so the new database scene has no vehicles
         * or measurements.
         */
        const newSceneId =
            await backend.createNewScene()

        alert(
            "Previous scene saved successfully:\n" +
            savedSceneId +
            "\n\nNew empty scene created:\n" +
            newSceneId
        )
    } catch (error) {
        /*
         * If the old scene was saved but creating the
         * new scene failed, reopen the saved scene so
         * the editor is not left in a broken state.
         */
        if (savedSceneId) {
            try {
                await backend.loadScene(
                    savedSceneId
                )
            } catch (restoreError) {
                console.error(
                    "Could not restore the saved scene:",
                    restoreError
                )
            }
        }

        alert(
            "Unable to create a new scene:\n" +
            error.message
        )
    } finally {
        resetButton.disabled = false
        resetButton.textContent =
            originalText
    }
}
$("resetBtn").onclick =
    () => resetScene()
    const clone = () => JSON.parse(JSON.stringify({ cars: state.cars, measurements: state.measurements, vehicleDistances: state.vehicleDistances }))
    const restore = s => { state.cars=s.cars.map(c=>Object.assign(state.newCar(c.vehicleData.type),c)); state.measurements=s.measurements||[]; state.vehicleDistances=s.vehicleDistances||{}; state.selectedCar=null; state.selectedCars=[]; refresh() }
    function pushHistory(){state.actionStack.push(clone());if(state.actionStack.length>50)state.actionStack.shift();state.redoStack=[]}
    function history(from,to){if(!from.length)return;to.push(clone());restore(from.pop())}
    function updateVehicleList(){const list=$("vehicleList");list.innerHTML="";state.cars.forEach(car=>{const item=document.createElement("div");item.className="vehicleItem";
        const confidence=typeof car.confidence==="number"?` — ${Math.round(car.confidence*100)}% confidence`:"";
        item.textContent=`${car.vehicleData.name} — ${car.vehicleData.type}${confidence}`;item.onclick=()=>{state.selectedCar=car;state.selectedCars=[car];refresh()};list.append(item)})}
    function loadVehicle(car){if(!car)return;$("vehicleName").value=car.vehicleData.name;$("vehicleModel").value=car.vehicleData.model;$("vehicleTypeInput").value=car.vehicleData.type;$("vehicleColor").value=car.vehicleData.color;$("vehiclePlate").value=car.vehicleData.plate;$("vehicleGuilty").checked=car.vehicleData.guilty;$("vehicleComment").value=car.vehicleData.comment;$("rotateSlider").value=$("angleInput").value=Math.round(car.rotation*180/Math.PI);$("scaleSlider").value=car.scale;$("sizeInput").value=(car.width*car.scale/SCALE_REAL).toFixed(2)}
    function refresh(){updateVehicleList();const car=state.selectedCar;const confidence=typeof car?.confidence==="number"?` | ${Math.round(car.confidence*100)}% confidence`:"";$("carInfo").textContent=car?`Selected: ${car.vehicleData.name} (${car.vehicleData.type}) | ${(car.width*car.scale/SCALE_REAL).toFixed(2)} m${confidence}`:"No vehicle selected";$("deleteVehicleBtn").disabled=!car;if(car)loadVehicle(car);measurements.updateList()}
    function updateZoom(){$("zoomDisplay").textContent=`Zoom: ${Math.round(state.worldScale*100)}%`}
    document
    .querySelectorAll(".vehicleBtn")
    .forEach(button => {
        button.onclick = async () => {
            if (state.aiMode) {
                return
            }

            const car =
                state.newCar(
                    button.dataset.type
                )

            button.disabled = true

            try {
                if (backend) {
                    await backend.addVehicle(car)
                }

                pushHistory()

                state.cars.push(car)
                state.selectedCar = car
                state.selectedCars = [car]

                refresh()
            } catch (error) {
                alert(
                    "Unable to add vehicle:\n" +
                    error.message
                )
            } finally {
                button.disabled = false
            }
        }
    })
    const rotate=d=>{if(!state.selectedCar||state.aiMode)return;pushHistory();state.selectedCar.rotation+=d;refresh()};$("rotateLeftBtn").onclick=()=>rotate(-Math.PI/180);$("rotateRightBtn").onclick=()=>rotate(Math.PI/180);$("flipVehicleBtn").onclick=()=>rotate(Math.PI)
    $("resizePlusBtn").onclick=()=>{if(state.selectedCar&&!state.aiMode){state.selectedCar.scale+=.1;refresh()}};$("resizeMinusBtn").onclick=()=>{if(state.selectedCar&&!state.aiMode){state.selectedCar.scale=Math.max(.2,state.selectedCar.scale-.1);refresh()}}
    $("rotateSlider").oninput=()=>{if(state.selectedCar){state.selectedCar.rotation=+$("rotateSlider").value*Math.PI/180;$("angleInput").value=$("rotateSlider").value}};$("angleInput").oninput=()=>{if(state.selectedCar){state.selectedCar.rotation=+$("angleInput").value*Math.PI/180;$("rotateSlider").value=$("angleInput").value}}
    $("scaleSlider").oninput=()=>{if(state.selectedCar){state.selectedCar.scale=+$("scaleSlider").value;refresh()}};$("sizeInput").oninput=()=>{if(state.selectedCar)state.selectedCar.scale=+$("sizeInput").value*SCALE_REAL/state.selectedCar.width}
    $("zoomInBtn").onclick=()=>{state.worldScale*=1.2;updateZoom()};$("zoomOutBtn").onclick=()=>{state.worldScale*=.8;updateZoom()};$("measureBtn").onclick=()=>{$("measureBtn").classList.toggle("active",state.measureMode=!state.measureMode)}
    const fields={vehicleModel:"model",vehicleTypeInput:"type",vehicleColor:"color",vehiclePlate:"plate",vehicleComment:"comment"};Object.entries(fields).forEach(([id,key])=>$(id).oninput=()=>{if(state.selectedCar)state.selectedCar.vehicleData[key]=$(id).value});$("vehicleName").oninput=()=>{if(state.selectedCar){state.selectedCar.vehicleData.name=$("vehicleName").value;updateVehicleList();notes.updateList()}};$("vehicleGuilty").onchange=()=>{if(state.selectedCar)state.selectedCar.vehicleData.guilty=$("vehicleGuilty").checked}
    const loc={locFile:["file_name"],locName:["name"],locDesc:["desc"],topWidth:["Top","w",Number],topLane:["Top","lanes",Number],bottomWidth:["Bottom","w",Number],bottomLane:["Bottom","lanes",Number],rightWidth:["Right","w",Number],rightLane:["Right","lanes",Number],leftWidth:["Left","w",Number],leftLane:["Left","lanes",Number],roundDiameter:["Roundabout",null,Number]};Object.entries(loc).forEach(([id,[a,b,cast]])=>$(id).oninput=()=>{const v=cast?cast($(id).value):$(id).value;if(b)state.locationData[a][b]=v;else state.locationData[a]=v});$("tjunctionCheck").onchange=()=>state.locationData.TJunction=$("tjunctionCheck").checked
    function loadLocation(){Object.entries(loc).forEach(([id,[a,b]])=>$(id).value=b?state.locationData[a]?.[b]??"":state.locationData[a]??"");$("tjunctionCheck").checked=state.locationData.TJunction;const sceneType=$("sceneType");sceneType.value=state.locationData.scene_type||"glavnaulica.png";sceneType.dispatchEvent(new Event("change"))}
const remove = async () => {
    const selectedCar =
        state.selectedCar

    if (!selectedCar) {
        return
    }

    const removedVehicleId =
        selectedCar.vehicleId

    const deleteButton =
        $("deleteVehicleBtn")

    deleteButton.disabled = true

    try {
        if (backend) {
            await backend.removeVehicle(
                removedVehicleId
            )
        }

        pushHistory()

        state.cars =
            state.cars.filter(
                car =>
                    car.vehicleId !==
                    removedVehicleId
            )

        state.measurements =
            state.measurements.filter(
                measurement =>
                    measurement
                        .fromVehicleId !==
                        removedVehicleId &&
                    measurement
                        .toVehicleId !==
                        removedVehicleId
            )

        state.vehicleDistances = {}
        state.selectedCar = null
        state.selectedCars = []

        refresh()
    } catch (error) {
        alert(
            "Unable to delete vehicle:\n" +
            error.message
        )
    } finally {
        deleteButton.disabled =
            !state.selectedCar
    }
}
$("deleteVehicleBtn").onclick =
    () => remove()
    document.onkeydown=e=>{if(e.key==="Escape"){state.selectedCar=null;state.selectedCars=[];refresh()}if(e.key==="Delete")remove();if(e.ctrlKey&&e.key.toLowerCase()==="z"){e.preventDefault();history(state.actionStack,state.redoStack)}if(e.ctrlKey&&e.key.toLowerCase()==="y"){e.preventDefault();history(state.redoStack,state.actionStack)}}
    refresh();
    return {
    pushHistory,
    refresh,
    updateZoom,
    loadLocation,
    setBackend
}
}
