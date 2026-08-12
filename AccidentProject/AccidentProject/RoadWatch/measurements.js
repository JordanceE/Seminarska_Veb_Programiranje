import { SCALE_REAL } from "./car.js"

export function createMeasurements(state) {
    let backend = null

function setBackend(value) {
    backend = value
}
    function recalculate(measurement) {
        if (
            measurement.x1 == null ||
            measurement.y1 == null ||
            measurement.x2 == null ||
            measurement.y2 == null
        ) {
            return
        }

        measurement.len =
            Math.hypot(
                measurement.x2 - measurement.x1,
                measurement.y2 - measurement.y1
            ) / SCALE_REAL
    }

    function draw() {
        const { ctx } = state

        state.measurements.forEach(measurement => {
            if (
                measurement.x1 == null ||
                measurement.y1 == null ||
                measurement.x2 == null ||
                measurement.y2 == null
            ) {
                return
            }

            if (
                !measurement.manual &&
                measurement.len == null
            ) {
                recalculate(measurement)
            }

            const vehicleToVehicle =
                measurement.type ===
                "VEHICLE_TO_VEHICLE"

            const color =
                vehicleToVehicle
                    ? "#00ffff"
                    : "yellow"

            ctx.save()
            ctx.strokeStyle = color
            ctx.fillStyle = color

            ctx.beginPath()
            ctx.moveTo(
                measurement.x1,
                measurement.y1
            )
            ctx.lineTo(
                measurement.x2,
                measurement.y2
            )
            ctx.stroke()

            const length =
                Number(measurement.len)

            if (Number.isFinite(length)) {
                ctx.fillText(
                    `${length.toFixed(2)} m`,
                    (
                        measurement.x1 +
                        measurement.x2
                    ) / 2,
                    (
                        measurement.y1 +
                        measurement.y2
                    ) / 2
                )
            }

            ctx.restore()
        })
    }

    /*
     * Kept temporarily because canvas.js already calls it.
     * Vehicle distances are now rendered by draw().
     */
    function drawVehicleDistances() {
    }

    function updateForMovedVehicles(movedCars) {
        const movedById = new Map(
            movedCars.map(car => [
                car.vehicleId,
                car
            ])
        )

        state.measurements.forEach(measurement => {
            let changed = false

            const fromCar =
                movedById.get(
                    measurement.fromVehicleId
                )

            const toCar =
                movedById.get(
                    measurement.toVehicleId
                )

            if (fromCar) {
                measurement.x1 = fromCar.x
                measurement.y1 = fromCar.y
                changed = true
            }

            if (toCar) {
                measurement.x2 = toCar.x
                measurement.y2 = toCar.y
                changed = true
            }

            if (changed) {
                measurement.manual = false
                recalculate(measurement)
            }
        })

        updateList()
    }

    function updateList() {
        const list =
            document.getElementById(
                "measureList"
            )

        list.innerHTML = ""

        state.measurements.forEach(
            measurement => {
                const length =
                    Number(measurement.len)

                if (!Number.isFinite(length)) {
                    return
                }

                const row =
                    document.createElement("div")

                row.style.cssText =
                    "display:flex;" +
                    "align-items:center;" +
                    "gap:6px"

                const span =
                    document.createElement("span")

                const vehicleToVehicle =
                    measurement.type ===
                    "VEHICLE_TO_VEHICLE"

                span.style.color =
                    vehicleToVehicle
                        ? "#00ffff"
                        : "yellow"

                span.textContent =
                    measurement.label ||
                    measurement.from ||
                    (
                        vehicleToVehicle
                            ? "Vehicle distance"
                            : "Measurement"
                    )

                const input =
                    document.createElement("input")

                input.type = "number"
                input.step = "0.01"
                input.value =
                    length.toFixed(2)

                input.style.width = "70px"

                input.oninput = () => {
                    const value =
                        Number(input.value)

                    if (Number.isFinite(value)) {
                        measurement.len = value
                        measurement.manual = true
                    }
                }
                const deleteButton =
    document.createElement("button")

deleteButton.type = "button"
deleteButton.className =
    "measurementDeleteButton"

deleteButton.textContent = "Delete"

deleteButton.onclick =
    async () => {
        deleteButton.disabled = true

        try {
            if (backend) {
                await backend
                    .removeMeasurement(
                        measurement
                            .measurementId
                    )
            }

            state.measurements =
                state.measurements.filter(
                    item =>
                        item.measurementId !==
                        measurement
                            .measurementId
                )

            updateList()
        } catch (error) {
            deleteButton.disabled = false

            alert(
                "Unable to delete measurement:\n" +
                error.message
            )
        }
    }

                row.append(
                    span,
                    input,
                    " m",
                    deleteButton
                )

                list.append(row)
            }
        )
    }

    return {
        draw,
        drawVehicleDistances,
        updateForMovedVehicles,
        updateList,
        setBackend
    }
}