export function initSavedScenes(backend) {
    console.log("[Saved Scenes] Initializing...");

    if (!backend) {
        throw new Error(
            "[Saved Scenes] Backend API object was not provided."
        );
    }

    if (typeof backend.searchScenes !== "function") {
        throw new Error(
            "[Saved Scenes] backend.searchScenes is not a function."
        );
    }

    const elements = {
        editor: document.getElementById("app"),
        savedPage: document.getElementById("savedScenesPage"),
        editorTab: document.getElementById("editorTab"),
        savedTab: document.getElementById("savedScenesTab"),
        searchInput: document.getElementById("sceneSearchInput"),
        statusSelect: document.getElementById("sceneStatusFilter"),
        searchButton: document.getElementById("searchScenesBtn"),
        refreshButton: document.getElementById("refreshScenesBtn"),
        resultContainer: document.getElementById("savedSceneResults"),
        previousButton: document.getElementById("previousScenesPage"),
        nextButton: document.getElementById("nextScenesPage"),
        pageInfo: document.getElementById("scenesPageInfo"),
        detailsPage: document.getElementById("sceneDetailsPage"),
        detailsTitle: document.getElementById("sceneDetailsTitle"),
        detailsContent: document.getElementById("sceneDetailsContent"),
        backToSavedButton:
    document.getElementById(
        "backToSavedScenesBtn"
    ),

finalizeSceneButton:
    document.getElementById(
        "finalizeAccidentSceneBtn"
    ),

archiveSceneButton:
    document.getElementById(
        "archiveAccidentSceneBtn"
    ),

deleteSceneButton:
    document.getElementById(
        "deleteAccidentSceneBtn"
    )
    };

    for (const [name, element] of Object.entries(elements)) {
        if (!element) {
            throw new Error(
                `[Saved Scenes] Missing HTML element: ${name}`
            );
        }
    }

    let currentPage = 0;
    let totalPages = 0;
    let viewedSceneId = null;
    let viewedScene = null;

    function showEditor() {
    elements.editor.hidden = false;
    elements.savedPage.hidden = true;
    elements.detailsPage.hidden = true;

    elements.editorTab.classList.add("active");
    elements.savedTab.classList.remove("active");
}

async function showSavedScenes(page = 0) {
    elements.editor.hidden = true;
    elements.savedPage.hidden = false;
    elements.detailsPage.hidden = true;

    elements.savedTab.classList.add("active");
    elements.editorTab.classList.remove("active");

    await loadPage(page);
}

    async function loadPage(page) {
        console.log(
            `[Saved Scenes] Loading page ${page}`
        );

        elements.resultContainer.innerHTML =
            "<p>Loading saved scenes...</p>";

        try {
            const result = await backend.searchScenes({
                query: elements.searchInput.value,
                status: elements.statusSelect.value,
                page,
                size: 12
            });

            console.log(
                "[Saved Scenes] Backend response:",
                result
            );

            renderResult(result, page);
        } catch (error) {
            console.error(
                "[Saved Scenes] Loading failed:",
                error
            );

            elements.resultContainer.innerHTML = `
                <p class="sceneError">
                    Unable to load accident scenes:
                    ${escapeHtml(error.message)}
                </p>
            `;
        }
    }

    function renderResult(result, requestedPage) {
        /*
         * Supports both:
         *
         * 1. Spring Page response:
         *    { content: [...], number: 0, totalPages: 1 }
         *
         * 2. Plain array response:
         *    [ ... ]
         */
        const scenes = Array.isArray(result)
            ? result
            : result?.content || [];

        currentPage = Array.isArray(result)
            ? 0
            : result?.number ?? requestedPage;

        totalPages = Array.isArray(result)
            ? (scenes.length > 0 ? 1 : 0)
            : result?.totalPages ?? 0;

        elements.resultContainer.innerHTML = "";

        if (scenes.length === 0) {
            elements.resultContainer.innerHTML =
                "<p>No saved accident scenes were found.</p>";

            updatePagination();
            return;
        }

        for (const scene of scenes) {
            elements.resultContainer.appendChild(
                createSceneCard(scene)
            );
        }

        updatePagination();
    }
    function updateLifecycleButtons(scene) {
    const status =
        scene?.status

    const vehicleCount =
        scene?.vehicles?.length || 0

    const canFinalize =
        (
            status === "DRAFT" ||
            status === "AI_ANALYZED"
        ) &&
        vehicleCount > 0

    const canArchive =
        status === "FINALIZED"

    elements.finalizeSceneButton.disabled =
        !canFinalize

    elements.archiveSceneButton.disabled =
        !canArchive

    elements.finalizeSceneButton.title =
        canFinalize
            ? "Finalize this accident scene"
            : (
                vehicleCount === 0
                    ? "A scene needs at least one vehicle before it can be finalized"
                    : `A ${status} scene cannot be finalized`
            )

    elements.archiveSceneButton.title =
        canArchive
            ? "Archive this finalized scene"
            : "Only finalized scenes can be archived"
}
    async function showSceneDetails(id) {
    viewedSceneId = id;

    elements.editor.hidden = true;
    elements.savedPage.hidden = true;
    elements.detailsPage.hidden = false;

    elements.detailsTitle.textContent =
        "Loading Accident Scene...";

    elements.detailsContent.innerHTML =
        "<p>Loading complete accident-scene information...</p>";

    elements.deleteSceneButton.disabled = true;
    elements.finalizeSceneButton.disabled =
    true

elements.archiveSceneButton.disabled =
    true
    try {
        const scene =
            await backend.getScene(id);

        viewedSceneId = scene.id;
        viewedScene = scene
updateLifecycleButtons(scene)

        renderSceneDetails(scene);

        elements.deleteSceneButton.disabled = false;
    } catch (error) {
        console.error(
            "[Saved Scenes] Unable to load details:",
            error
        );

        elements.detailsTitle.textContent =
            "Unable to Load Scene";

        elements.detailsContent.innerHTML = `
            <p class="sceneError">
                ${escapeHtml(error.message)}
            </p>
        `;
    }
}
function renderSceneDetails(scene) {
    const location =
        scene.locationInfo || {};

    const vehicles =
        scene.vehicles || [];

    const measurements =
        scene.measurements || [];

    elements.detailsTitle.textContent =
        location.name ||
        scene.name ||
        "Unnamed Accident Scene";

    const vehicleRows = vehicles.length
        ? vehicles.map(vehicle => `
            <tr>
                <td>
                    ${escapeHtml(vehicle.vehicleId)}
                </td>

                <td>
                    ${escapeHtml(vehicle.name)}
                </td>

                <td>
                    ${escapeHtml(vehicle.type)}
                </td>

                <td>
                    ${escapeHtml(vehicle.model || "—")}
                </td>

                <td>
                    ${escapeHtml(vehicle.color || "—")}
                </td>

                <td>
                    ${escapeHtml(vehicle.plate || "—")}
                </td>

                <td>
                    X: ${formatNumber(vehicle.position?.x)},
                    Y: ${formatNumber(vehicle.position?.y)}
                </td>

                <td>
                    ${formatNumber(vehicle.width)}
                    ×
                    ${formatNumber(vehicle.height)}
                </td>

                <td>
                    ${formatNumber(vehicle.rotation)}°
                </td>

                <td>
                    ${vehicle.guilty ? "Yes" : "No"}
                </td>

                <td>
                    ${escapeHtml(
                        vehicle.comment ||
                        vehicle.note ||
                        "—"
                    )}
                </td>
                <td>
                    ${vehicle.confidence}
                </td>
            </tr>
        `).join("")
        : `
            <tr>
                <td colspan="11">
                    No vehicles stored.
                </td>
            </tr>
        `;

    const measurementRows = measurements.length
        ? measurements.map(measurement => `
            <tr>
                <td>
                    ${escapeHtml(
                        measurement.measurementId
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        measurement.type || "—"
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        measurement.fromVehicleId ||
                        "—"
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        measurement.toVehicleId ||
                        "—"
                    )}
                </td>

                <td>
                    (${formatNumber(measurement.x1)},
                    ${formatNumber(measurement.y1)})
                </td>

                <td>
                    (${formatNumber(measurement.x2)},
                    ${formatNumber(measurement.y2)})
                </td>

                <td>
                    ${formatNumber(
                        measurement.lengthMeters
                    )} m
                </td>

                <td>
                    ${escapeHtml(
                        measurement.label || "—"
                    )}
                </td>
            </tr>
        `).join("")
        : `
            <tr>
                <td colspan="8">
                    No measurements stored.
                </td>
            </tr>
        `;

    elements.detailsContent.innerHTML = `
        <div class="sceneDetailsGrid">

            <section class="sceneDetailCard">
                <h2>General Information</h2>

                ${detailRow("Scene ID", scene.id)}

                ${detailRow(
                    "Road layout",
                    readableLayout(
                        scene.roadLayoutType
                    )
                )}

                ${detailRow(
                    "Status",
                    scene.status
                )}

                ${detailRow(
                    "Created",
                    formatDate(scene.createdAt)
                )}

                ${detailRow(
                    "Last updated",
                    formatDate(scene.updatedAt)
                )}

                ${detailRow(
                    "AI confidence",
                    scene.aiConfidence == null
                        ? "—"
                        : `${Math.round(
                            scene.aiConfidence * 100
                        )}%`
                )}

                ${detailRow(
                    "AI summary",
                    scene.aiSummary || "—"
                )}
            </section>

            <section class="sceneDetailCard">
                <h2>Location Information</h2>

                ${detailRow(
                    "Name",
                    location.name || "—"
                )}

                ${detailRow(
                    "Filename",
                    location.fileName || "—"
                )}

                ${detailRow(
                    "Description",
                    location.description || "—"
                )}

                ${detailRow(
                    "Top street",
                    roadDescription(
                        location.topRoadWidth,
                        location.topRoadLanes
                    )
                )}

                ${detailRow(
                    "Bottom street",
                    roadDescription(
                        location.bottomRoadWidth,
                        location.bottomRoadLanes
                    )
                )}

                ${detailRow(
                    "Left street",
                    roadDescription(
                        location.leftRoadWidth,
                        location.leftRoadLanes
                    )
                )}

                ${detailRow(
                    "Right street",
                    roadDescription(
                        location.rightRoadWidth,
                        location.rightRoadLanes
                    )
                )}

                ${detailRow(
                    "Roundabout diameter",
                    location.roundaboutDiameter == null
                        ? "—"
                        : `${location.roundaboutDiameter} m`
                )}

                ${detailRow(
                    "T-junction",
                    location.tJunction ? "Yes" : "No"
                )}
            </section>

        </div>

        <section class="sceneDetailCard sceneTableCard">
            <h2>
                Vehicles (${vehicles.length})
            </h2>

            <div class="sceneTableWrapper">
                <table class="sceneDetailsTable">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Type</th>
                            <th>Model</th>
                            <th>Color</th>
                            <th>Plate</th>
                            <th>Position</th>
                            <th>Size</th>
                            <th>Rotation</th>
                            <th>Guilty</th>
                            <th>Notes</th>
                            <th>Confidence</th>
                        </tr>
                    </thead>

                    <tbody>
                        ${vehicleRows}
                    </tbody>
                </table>
            </div>
        </section>

        <section class="sceneDetailCard sceneTableCard">
            <h2>
                Measurements (${measurements.length})
            </h2>

            <div class="sceneTableWrapper">
                <table class="sceneDetailsTable">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Type</th>
                            <th>From vehicle</th>
                            <th>To vehicle</th>
                            <th>Start</th>
                            <th>End</th>
                            <th>Length</th>
                            <th>Label</th>
                        </tr>
                    </thead>

                    <tbody>
                        ${measurementRows}
                    </tbody>
                </table>
            </div>
        </section>
    `;
}
function detailRow(label, value) {
    return `
        <div class="detailRow">
            <strong>
                ${escapeHtml(label)}
            </strong>

            <span>
                ${escapeHtml(value)}
            </span>
        </div>
    `;
}

function roadDescription(
    width,
    lanes
) {
    if (
        width == null &&
        lanes == null
    ) {
        return "—";
    }

    const widthText =
        width == null
            ? "unknown width"
            : `${width} m`;

    const lanesText =
        lanes == null
            ? "unknown lanes"
            : `${lanes} lane${lanes === 1 ? "" : "s"}`;

    return `${widthText}, ${lanesText}`;
}

function formatNumber(value) {
    const number = Number(value);

    if (!Number.isFinite(number)) {
        return "—";
    }

    return number.toFixed(2);
}
function readableLayout(layout) {
    if (!layout) {
        return "Unknown";
    }

    return layout
        .toLowerCase()
        .replaceAll("_", " ")
        .replace(/\b\w/g, letter =>
            letter.toUpperCase()
        );
}

function formatDate(value) {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "—";
    }

    return date.toLocaleString();
}
    function createSceneCard(scene) {
    const card = document.createElement("article");
    card.className = "savedSceneCard";

    const title = document.createElement("h3");
    title.textContent =
        scene.name ||
        scene.locationInfo?.name ||
        "Unnamed accident scene";

    const id = document.createElement("code");
    id.textContent = scene.id || "Unknown ID";

    const details = document.createElement("div");
    details.className = "sceneCardDetails";

    const layout = document.createElement("p");
    layout.innerHTML = `
        <strong>Layout:</strong>
        ${escapeHtml(
            readableLayout(
                scene.roadLayoutType || "UNKNOWN"
            )
        )}
    `;

    const status = document.createElement("p");
    status.innerHTML = `
        <strong>Status:</strong>
        ${escapeHtml(scene.status || "Unknown")}
    `;

    const confidence = document.createElement("p");

    confidence.innerHTML = `
        <strong>AI confidence:</strong>
        ${
            scene.aiConfidence == null
                ? "—"
                : `${Math.round(
                    scene.aiConfidence * 100
                )}%`
        }
    `;

    const created = document.createElement("p");
    created.innerHTML = `
        <strong>Created:</strong>
        ${escapeHtml(
            formatDate(scene.createdAt)
        )}
    `;

    const updated = document.createElement("p");
    updated.innerHTML = `
        <strong>Updated:</strong>
        ${escapeHtml(
            formatDate(scene.updatedAt)
        )}
    `;

    details.append(
        layout,
        status,
        confidence,
        created,
        updated
    );

    const openButton = document.createElement("button");
    openButton.type = "button";
    openButton.textContent = "Open in Editor";

    openButton.addEventListener(
        "click",
        async () => {
            openButton.disabled = true;
            openButton.textContent = "Loading...";

            try {
                await backend.loadScene(scene.id);
                showEditor();
            } catch (error) {
                console.error(
                    "[Saved Scenes] Unable to open scene:",
                    error
                );

                alert(
                    `Unable to open scene:\n${error.message}`
                );
            } finally {
                openButton.disabled = false;
                openButton.textContent =
                    "Open in Editor";
            }
        }
    );

    const viewButton = document.createElement("button");
    viewButton.type = "button";
    viewButton.className = "viewSceneButton";
    viewButton.textContent = "View Accident Scene";

    viewButton.addEventListener(
        "click",
        async () => {
            viewButton.disabled = true;
            viewButton.textContent = "Loading...";

            try {
                await showSceneDetails(scene.id);
            } catch (error) {
                console.error(
                    "[Saved Scenes] Unable to show details:",
                    error
                );

                alert(
                    `Unable to view scene:\n${error.message}`
                );
            } finally {
                viewButton.disabled = false;
                viewButton.textContent =
                    "View Accident Scene";
            }
        }
    );

    const actions = document.createElement("div");
    actions.className = "savedSceneActions";

    actions.append(
        openButton,
        viewButton
    );

    card.append(
        title,
        id,
        details,
        actions
    );

    return card;
}

    function updatePagination() {
        elements.pageInfo.textContent =
            totalPages === 0
                ? "Page 0 of 0"
                : `Page ${currentPage + 1} of ${totalPages}`;

        elements.previousButton.disabled =
            currentPage <= 0;

        elements.nextButton.disabled =
            totalPages === 0 ||
            currentPage >= totalPages - 1;
    }

    function escapeHtml(value) {
        const element = document.createElement("div");
        element.textContent = String(value ?? "");
        return element.innerHTML;
    }

    elements.savedTab.addEventListener(
    "click",
    () => showSavedScenes(0)
);

    elements.editorTab.addEventListener(
        "click",
        showEditor
    );
    elements.backToSavedButton.addEventListener(
    "click",
    () => showSavedScenes(currentPage)
);
    elements.finalizeSceneButton
    .addEventListener(
        "click",
        async () => {
            if (!viewedSceneId) {
                return
            }

            const confirmed =
                window.confirm(
                    "Finalize this accident scene?\n\n" +
                    "After finalization, the scene can no longer be edited."
                )

            if (!confirmed) {
                return
            }

            const button =
                elements.finalizeSceneButton

            const originalText =
                button.textContent

            button.disabled = true
            button.textContent =
                "Finalizing..."

            try {
                await backend.finalizeScene(
                    viewedSceneId
                )

                await showSceneDetails(
                    viewedSceneId
                )
            } catch (error) {
                alert(
                    "Unable to finalize scene:\n" +
                    error.message
                )

                updateLifecycleButtons(
                    viewedScene
                )
            } finally {
                button.textContent =
                    originalText
            }
        }
    )
    elements.archiveSceneButton
    .addEventListener(
        "click",
        async () => {
            if (!viewedSceneId) {
                return
            }

            const confirmed =
                window.confirm(
                    "Archive this accident scene?"
                )

            if (!confirmed) {
                return
            }

            const button =
                elements.archiveSceneButton

            const originalText =
                button.textContent

            button.disabled = true
            button.textContent =
                "Archiving..."

            try {
                await backend.archiveScene(
                    viewedSceneId
                )

                await showSceneDetails(
                    viewedSceneId
                )
            } catch (error) {
                alert(
                    "Unable to archive scene:\n" +
                    error.message
                )

                updateLifecycleButtons(
                    viewedScene
                )
            } finally {
                button.textContent =
                    originalText
            }
        }
    )
    elements.deleteSceneButton.addEventListener(
    "click",
    async () => {
        if (!viewedSceneId) {
            return;
        }

        const confirmed = window.confirm(
            "Are you sure you want to permanently delete " +
            "this accident scene?\n\n" +
            viewedSceneId +
            "\n\nThis action cannot be undone."
        );

        if (!confirmed) {
            return;
        }

        const originalText =
            elements.deleteSceneButton.textContent;

        elements.deleteSceneButton.disabled = true;
        elements.deleteSceneButton.textContent =
            "Deleting...";

        try {
            await backend.deleteScene(
                viewedSceneId
            );

            viewedSceneId = null;

            alert(
                "The accident scene was deleted successfully."
            );

            await showSavedScenes(0);
        } catch (error) {
            console.error(
                "[Saved Scenes] Delete failed:",
                error
            );

            alert(
                `Unable to delete the accident scene:\n${error.message}`
            );
        } finally {
            elements.deleteSceneButton.disabled = false;
            elements.deleteSceneButton.textContent =
                originalText;
        }
    }
);
    elements.searchButton.addEventListener(
        "click",
        () => loadPage(0)
    );

    elements.refreshButton.addEventListener(
        "click",
        () => loadPage(currentPage)
    );

    elements.statusSelect.addEventListener(
        "change",
        () => loadPage(0)
    );

    elements.searchInput.addEventListener(
        "keydown",
        event => {
            if (event.key === "Enter") {
                loadPage(0);
            }
        }
    );

    elements.previousButton.addEventListener(
        "click",
        () => {
            if (currentPage > 0) {
                loadPage(currentPage - 1);
            }
        }
    );

    elements.nextButton.addEventListener(
        "click",
        () => {
            if (currentPage < totalPages - 1) {
                loadPage(currentPage + 1);
            }
        }
    );

    showEditor();

    console.log(
        "[Saved Scenes] Successfully initialized."
    );

    return {
        showEditor,
        showSavedScenes,
        loadPage
    };
}