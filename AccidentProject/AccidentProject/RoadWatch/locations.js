import { API_ORIGIN, ROAD_LAYOUT_TO_BACKEND, toBackendLocation } from "./backend-api.js"
import { showPage } from "./navigation.js"
import { LOCATION_PRESETS } from "./location-presets.js"

const $ = id => document.getElementById(id)
const fields = [
    ["name", "Location name", "text", true],
    ["description", "Description", "text"], ["backgroundFileName", "Road layout", "select"],
    ["topRoadWidth", "Top road width (m)", "number"], ["topRoadLanes", "Top road lanes", "integer"],
    ["bottomRoadWidth", "Bottom road width (m)", "number"], ["bottomRoadLanes", "Bottom road lanes", "integer"],
    ["leftRoadWidth", "Left road width (m)", "number"], ["leftRoadLanes", "Left road lanes", "integer"],
    ["rightRoadWidth", "Right road width (m)", "number"], ["rightRoadLanes", "Right road lanes", "integer"],
    ["roundaboutDiameter", "Roundabout diameter (m)", "number"], ["tJunction", "T-junction", "checkbox"]
]

export function initLocations(state, backend) {
    let page = 0, totalPages = 0, libraryRequest = 0, previewUrl = null, formSource = null
    const pickers = []
    const deletedIds = new Set()
    const message = (id, text) => { $(id).textContent = text }
    const label = location => `${location.name} (${location.id.slice(-8)})`
    const option = (value, text) => { const item = document.createElement("option"); item.value = value; item.textContent = text; return item }

    function picker(prefix, selectId, moreId, onSelect) {
        const select = $(selectId), search = $(`${prefix}Search`), more = $(moreId)
        const placeholder = select.options[0].textContent
        let nextPage = 0, generation = 0, debounce
        async function load(append = false) {
            const token = ++generation
            more.disabled = true
            try {
                const result = await backend.searchLocations({ query: search.value, page: append ? nextPage : 0 })
                if (generation !== token) return
                const previous = select.selectedOptions[0]?.cloneNode(true)
                const selected = deletedIds.has(select.value) ? "" : select.value
                if (!append) select.replaceChildren(option("", placeholder))
                if (selected && ![...select.options].some(o => o.value === selected) && previous) select.append(previous)
                for (const location of result.content) {
                    if (deletedIds.has(location.id)) continue
                    if (![...select.options].some(o => o.value === location.id)) select.append(option(location.id, label(location)))
                }
                select.value = selected
                nextPage = result.number + 1
                more.hidden = nextPage >= result.totalPages
                message(`${prefix}Message`, result.totalElements ? `${result.totalElements} matching location(s)` : "No matching locations")
            } catch (error) {
                if (generation === token) message(`${prefix}Message`, error.message)
            } finally { if (generation === token) more.disabled = false }
        }
        search.addEventListener("input", () => { clearTimeout(debounce); debounce = setTimeout(() => load(), 250) })
        more.onclick = () => load(true)
        select.addEventListener("change", async () => {
            try { await onSelect(select.value) } catch (error) { message(`${prefix}Message`, error.message) }
        })
        const set = location => {
            if (deletedIds.has(location.id)) return
            if (![...select.options].some(o => o.value === location.id)) select.append(option(location.id, label(location)))
            select.value = location.id
        }
        pickers.push(load)
        load()
        return { set }
    }

    const editorPicker = picker("editorLocation", "editorLocationSelect", "editorLocationsMore", async id => {
        if (id) {
            const location = await backend.getLocation(id)
            if (deletedIds.has(id) || $("editorLocationSelect").value !== id) return
            backend.applyLocation(location)
            message("editorLocationMessage", "Location loaded. Your vehicles and measurements are unchanged.")
        } else {
            delete state.locationData.locationId
            delete state.locationData.templateSignature
        }
    })
    picker("sceneLocation", "sceneLocationFilter", "sceneLocationsMore", () => {})

    window.addEventListener("location-selected", event => {
        editorPicker.set(event.detail)
        message("editorLocationMessage", `Selected: ${event.detail.name}`)
    })
    window.addEventListener("location-cleared", () => {
        $("editorLocationSelect").value = ""
        message("editorLocationMessage", "Enter location details or select a saved template.")
    })
    window.addEventListener("locations-changed", () => pickers.forEach(load => load()))
    window.addEventListener("location-deleted", event => {
        const id = event.detail.id
        deletedIds.add(id)
        for (const selectId of ["editorLocationSelect", "sceneLocationFilter"]) {
            const select = $(selectId), selected = select.value === id
            for (const item of [...select.options]) if (item.value === id) item.remove()
            if (selected) {
                select.value = ""
                select.dispatchEvent(new Event("change"))
            }
        }
        if (formSource === id) {
            formSource = null
            preview($("locationFormPhoto").files[0] || builtInPreview())
            message("locationFormMessage", "The source location was deleted. Choose a new picture or use the road layout.")
        }
    })
    $("editorLocationPhoto").onchange = event => {
        state.locationPhotoFile = event.target.files[0] || null
        message("editorLocationMessage", state.locationPhotoFile ? "Picture will upload when you save the location or accident." : "No new picture selected.")
    }
    $("clearLocationSelection").onclick = () => {
        delete state.locationData.locationId
        delete state.locationData.templateSignature
        window.dispatchEvent(new Event("location-cleared"))
    }
    $("saveLocationFromEditor").onclick = async event => {
        event.target.disabled = true
        try {
            const saved = await backend.saveEditorLocation()
            message("editorLocationMessage", `Saved / reused: ${saved.name}`)
        } catch (error) { message("editorLocationMessage", error.message) }
        finally { event.target.disabled = false }
    }

    for (const [key, title, type, required] of fields) {
        const wrapper = document.createElement("label")
        wrapper.textContent = title
        const input = document.createElement(type === "select" ? "select" : "input")
        input.id = `newLocation_${key}`
        if (type === "select") {
            for (const source of $("sceneType").options) input.append(source.cloneNode(true))
        } else {
            input.type = type === "integer" ? "number" : type
            if (type === "number" || type === "integer") { input.min = type === "integer" ? "1" : "0.01"; input.step = type === "integer" ? "1" : "any" }
            if (type === "text") input.maxLength = 255
        }
        input.required = Boolean(required)
        wrapper.append(input)
        $("locationFormFields").append(wrapper)
    }
    // Use the editor's labels and order so both screens expose the same eight layouts.
    for (const source of $("sceneType").options) {
        if (LOCATION_PRESETS[source.value]) $("locationPreset").append(source.cloneNode(true))
    }

    function fillForm(location) {
        for (const [key, , type] of fields) {
            const input = $(`newLocation_${key}`)
            if (type === "checkbox") input.checked = Boolean(location[key])
            else input.value = location[key] ?? (type === "select" ? "glavnaulica.png" : "")
        }
    }
    function readForm() {
        const value = {}
        for (const [key, , type] of fields) {
            const input = $(`newLocation_${key}`)
            value[key] = type === "checkbox" ? input.checked :
                ["number", "integer"].includes(type) ? (input.value === "" ? null : Number(input.value)) : input.value.trim() || null
        }
        value.roadLayoutType = ROAD_LAYOUT_TO_BACKEND[value.backgroundFileName]
        if (formSource && !$("locationFormPhoto").files[0]) value.photoSourceLocationId = formSource
        return value
    }
    function preview(source) {
        if (previewUrl) URL.revokeObjectURL(previewUrl)
        previewUrl = null
        if (source instanceof Blob) { previewUrl = URL.createObjectURL(source); source = previewUrl }
        $("locationFormPreview").hidden = !source
        if (source) $("locationFormPreview").src = source
        else $("locationFormPreview").removeAttribute("src")
    }
    function builtInPreview() {
        return `assets/${$("newLocation_backgroundFileName").value}`
    }
    function addPage(location = null) {
        formSource = location?.photoUrl ? location.id : null
        $("locationForm").reset()
        fillForm(location || {})
        preview(location?.photoUrl ? new URL(location.photoUrl, API_ORIGIN).href : location ? builtInPreview() : null)
        message("locationFormMessage", location ? "Edit this template to save a separate location. The original remains available." : "")
        showPage("addLocationPage", "addLocationTab")
    }
    $("locationFormPhoto").onchange = event => preview(event.target.files[0] ||
        (formSource ? `${API_ORIGIN}/api/locations/${encodeURIComponent(formSource)}/photo` : builtInPreview()))
    $("newLocation_backgroundFileName").onchange = () => {
        $("locationPreset").value = ""
        formSource = null
        $("locationFormPhoto").value = ""
        $("newLocation_tJunction").checked = $("newLocation_backgroundFileName").value === "t-junction.png"
        preview(builtInPreview())
    }
    $("locationPreset").onchange = event => {
        const background = event.target.value
        fillForm({ ...LOCATION_PRESETS[background], backgroundFileName: background || "glavnaulica.png",
            name: $("newLocation_name").value,
            description: $("newLocation_description").value })
        formSource = null
        $("locationFormPhoto").value = ""
        preview(background ? builtInPreview() : null)
    }
    $("locationForm").onsubmit = async event => {
        event.preventDefault()
        $("submitLocation").disabled = true
        try {
            const result = await backend.saveLocation(readForm(), $("locationFormPhoto").files[0] || null)
            showPage("savedLocationsPage", "savedLocationsTab")
            $("locationsSearch").value = result.name
            await loadLibrary(0)
            message("locationsMessage", `Saved / reused: ${result.name}. Choose “Use in Editor” to load it.`)
        } catch (error) { message("locationFormMessage", error.message) }
        finally { $("submitLocation").disabled = false }
    }

    async function loadLibrary(requestedPage) {
        const token = ++libraryRequest
        message("locationsMessage", "Loading locations…")
        try {
            const result = await backend.searchLocations({ query: $("locationsSearch").value, page: requestedPage, size: 12 })
            if (token !== libraryRequest) return
            if (requestedPage > 0 && requestedPage >= result.totalPages) {
                return loadLibrary(Math.max(0, result.totalPages - 1))
            }
            page = result.number; totalPages = result.totalPages
            $("locationResults").replaceChildren()
            for (const location of result.content) {
                const card = document.createElement("article"); card.className = "locationCard"
                card.dataset.locationId = location.id
                const title = document.createElement("h2"); title.textContent = location.name
                const description = document.createElement("p"); description.textContent = location.description || "No description"
                const photo = document.createElement("img"); photo.className = "locationPreview"; photo.alt = `Road layout for ${location.name}`
                photo.src = location.photoUrl ? new URL(location.photoUrl, API_ORIGIN).href : `assets/${location.backgroundFileName || Object.keys(ROAD_LAYOUT_TO_BACKEND).find(key => ROAD_LAYOUT_TO_BACKEND[key] === location.roadLayoutType) || "glavnaulica.png"}`
                const details = document.createElement("dl")
                for (const [key, caption] of fields.filter(([key]) => !["name", "description", "backgroundFileName"].includes(key))) {
                    const dt = document.createElement("dt"), dd = document.createElement("dd")
                    dt.textContent = caption; dd.textContent = location[key] == null ? "—" : String(location[key]); details.append(dt, dd)
                }
                const use = document.createElement("button"); use.type = "button"; use.textContent = "Use in Editor"
                use.onclick = () => { backend.applyLocation(location); showPage("app", "editorTab") }
                const copy = document.createElement("button"); copy.type = "button"; copy.textContent = "Create from this template"; copy.onclick = () => addPage(location)
                const remove = document.createElement("button")
                remove.type = "button"; remove.className = "dangerButton"; remove.textContent = "Delete Location"
                const feedback = document.createElement("p"); feedback.setAttribute("role", "status")
                remove.onclick = async () => {
                    if (!window.confirm(`Delete location "${location.name}"? This cannot be undone. Locations used by saved accidents cannot be deleted.`)) return
                    remove.disabled = use.disabled = copy.disabled = true
                    feedback.textContent = "Deleting location…"
                    try {
                        await backend.deleteLocation(location.id)
                        await loadLibrary(page)
                        message("locationsMessage", `Deleted: ${location.name}.`)
                    } catch (error) {
                        feedback.textContent = error.message
                    } finally { remove.disabled = use.disabled = copy.disabled = false }
                }
                card.append(title, photo, description, details, use, copy, remove, feedback)
                $("locationResults").append(card)
            }
            message("locationsMessage", result.totalElements ? `${result.totalElements} location(s)` : "No saved locations. Add your first location using the button above.")
            $("locationsPageInfo").textContent = `Page ${totalPages ? page + 1 : 0} of ${totalPages}`
            $("locationsPrevious").disabled = page === 0
            $("locationsNext").disabled = page + 1 >= totalPages
        } catch (error) { if (token === libraryRequest) message("locationsMessage", error.message) }
    }
    $("savedLocationsTab").onclick = () => { showPage("savedLocationsPage", "savedLocationsTab"); loadLibrary(0) }
    $("addLocationTab").onclick = () => addPage()
    $("uploadLocationButton").onclick = () => addPage()
    $("locationsSearchButton").onclick = () => loadLibrary(0)
    $("locationsSearch").onkeydown = event => { if (event.key === "Enter") loadLibrary(0) }
    $("locationsPrevious").onclick = () => { if (page > 0) loadLibrary(page - 1) }
    $("locationsNext").onclick = () => { if (page + 1 < totalPages) loadLibrary(page + 1) }
}
