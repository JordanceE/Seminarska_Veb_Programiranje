export function initStorage(state, { ui, notes, backend }) {
    const sceneData = () => ({
        location: state.locationData,
        cars: state.cars,
        measurements: state.measurements,
        vehicleDistances: state.vehicleDistances
    })
    const saveDatabaseButton =
    document.getElementById(
        "saveDatabaseBtn"
    )

saveDatabaseButton.onclick =
    async () => {
        const originalText =
            saveDatabaseButton.textContent

        saveDatabaseButton.disabled = true
        saveDatabaseButton.textContent =
            "Saving..."

        try {
            const id =
                await backend.saveCurrentScene()

            alert(
                `Accident scene saved successfully.\n${id}`
            )
        } catch (error) {
            alert(
                `Unable to save accident scene:\n${error.message}`
            )
        } finally {
            saveDatabaseButton.disabled = false
            saveDatabaseButton.textContent =
                originalText
        }
    }

const saveAsNewButton =
    document.getElementById(
        "saveAsNewDatabaseBtn"
    )

saveAsNewButton.onclick =
    async () => {
        const originalText =
            saveAsNewButton.textContent

        saveAsNewButton.disabled = true
        saveAsNewButton.textContent =
            "Saving..."

        try {
            const id =
                await backend.saveAsNewScene()

            alert(
                `New accident scene created.\n${id}`
            )
        } catch (error) {
            alert(
                `Unable to create accident scene:\n${error.message}`
            )
        } finally {
            saveAsNewButton.disabled = false
            saveAsNewButton.textContent =
                originalText
        }
    }
    const download = (name, type, content) => {
        const link = document.createElement("a")
        const url = URL.createObjectURL(new Blob([content], { type }))
        link.href = url
        link.download = name
        link.click()
        URL.revokeObjectURL(url)
    }

    const validateScene = data => {
        if (!data || typeof data !== "object" || Array.isArray(data)) {
            return "Invalid scene file: expected a JSON object."
        }
        if (!data.cars || !Array.isArray(data.cars)) {
            return "Invalid scene file: missing cars array."
        }
        if (data.measurements !== undefined && !Array.isArray(data.measurements)) {
            return "Invalid scene file: measurements must be an array."
        }
        if (data.vehicleDistances !== undefined &&
            (!data.vehicleDistances ||
                typeof data.vehicleDistances !== "object" ||
                Array.isArray(data.vehicleDistances))) {
            return "Invalid scene file: vehicleDistances must be an object."
        }
        if (data.vehicleDistances &&
            Object.values(data.vehicleDistances).some(distance =>
                typeof distance !== "number" || !Number.isFinite(distance))) {
            return "Invalid scene file: vehicle distances must be finite numbers."
        }
        if (data.location !== undefined &&
            (!data.location || typeof data.location !== "object" || Array.isArray(data.location))) {
            return "Invalid scene file: location must be an object."
        }

        const invalidCar = data.cars.find(car =>
            !car ||
            typeof car !== "object" ||
            !car.vehicleData ||
            typeof car.vehicleData !== "object" ||
            typeof car.vehicleData.type !== "string"
        )
        if (invalidCar) {
            return "Invalid scene file: every car must contain vehicleData with a type."
        }

        return null
    }

    document.getElementById("saveBtn").onclick = () =>
        download("scene.json", "application/json", JSON.stringify(sceneData(), null, 2))

    document.getElementById("downloadAIJsonBtn").onclick = () =>
        download("ai_scene.json", "application/json", JSON.stringify(sceneData(), null, 2))

    document.getElementById("savePhotoBtn").onclick = () => {
        const link = document.createElement("a")
        link.download = "scene.png"
        link.href = state.canvas.toDataURL("image/png")
        link.click()
    }

    const input = document.getElementById("loadInput")
    document.getElementById("loadBtn").onclick = () => input.click()

    input.onchange = () => {
        const file = input.files[0]
        if (!file) return

        const reader = new FileReader()
        reader.onload = () => {
            try {
                const data = JSON.parse(reader.result)
                const validationError = validateScene(data)
                if (validationError) {
                    alert(validationError)
                    return
                }

                const loadedCars = data.cars.map(car =>
                    Object.assign(state.newCar(car.vehicleData.type), car)
                )

                state.locationData = data.location || state.locationData
                state.measurements = (data.measurements || []).map(measurement => {
                    const legacyVehicleName = measurement.fromVehicleName || measurement.from || null
                    const sourceCar = loadedCars.find(car =>
                        car.vehicleData.name === legacyVehicleName
                    )

                    return {
                        ...measurement,
                        id: measurement.id || crypto.randomUUID(),
                        fromVehicleId: measurement.fromVehicleId ?? sourceCar?.id ?? null,
                        fromVehicleName: legacyVehicleName,
                        manual: Boolean(measurement.manual)
                    }
                })
                state.vehicleDistances = data.vehicleDistances || {}
                state.cars = loadedCars
                state.selectedCar = null
                state.selectedCars = []
                ui.refresh()
                ui.loadLocation()
                notes.updateList()
            } catch (error) {
                alert("Invalid scene file: the file does not contain valid JSON.")
            } finally {
                input.value = ""
            }
        }
        reader.onerror = () => {
            alert("Unable to read the selected scene file.")
            input.value = ""
        }
        reader.readAsText(file)
    }
}
