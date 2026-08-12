export function initAI(
    state,
    {
        ui,
        backend
    }
) {
    const popup = text => {
        const div =
            document.createElement("div")

        div.className =
            "aiPopup"

        const content =
            document.createElement("div")

        content.className =
            "popupContent"

        const span =
            document.createElement("span")

        span.innerHTML =
            text.replace(/\n/g, "<br>")

        const close =
            document.createElement("button")

        close.className =
            "popupClose"

        close.textContent =
            "✕"

        close.onclick =
            () => div.remove()

        content.append(
            span,
            close
        )

        div.append(content)

        document.body.append(div)
    }

    const fileInput =
        document.getElementById(
            "imageUpload"
        )

    const preview =
        document.getElementById(
            "aiPreview"
        )

    let previewUrl = null

    fileInput.onchange =
        () => {
            if (previewUrl) {
                URL.revokeObjectURL(
                    previewUrl
                )

                previewUrl = null
            }

            const file =
                fileInput.files[0]

            if (!file) {
                preview.removeAttribute(
                    "src"
                )

                return
            }

            previewUrl =
                URL.createObjectURL(file)

            preview.src =
                previewUrl
        }

    const button =
        document.getElementById(
            "analyzeBtn"
        )

    button.onclick =
        async () => {
            const file =
                fileInput.files[0]

            if (!file) {
                popup("Select image first")
                return
            }

            const originalText =
                button.textContent

            button.textContent =
                "Analyzing..."

            button.disabled = true

            button.classList.add(
                "aiAnalyzing"
            )

            try {
                const result =
                    await backend
                        .analyzeImage(file)

                state.aiMode = true

                ui.refresh()

                popup(
                    `AI analysis complete\n` +
                    `Vehicles detected: ${
                        result.cars?.length || 0
                    }\n` +
                    `AI confidence: ${
                        Math.round(
                            (result.confidence || 0) *
                            100
                        )
                    }%`
                )
            } catch (error) {
                popup(
                    `AI analysis failed\n${error.message}`
                )
            } finally {
                button.textContent =
                    originalText

                button.disabled = false

                button.classList.remove(
                    "aiAnalyzing"
                )
            }
        }

    const editButton =
        document.getElementById(
            "editModeBtn"
        )

    editButton.onclick =
        () => {
            state.aiMode =
                !state.aiMode

            editButton.textContent =
                state.aiMode
                    ? "Switch to Edit Mode"
                    : "Switch to AI Mode"

            popup(
                state.aiMode
                    ? "AI mode enabled.\nManual editing disabled."
                    : "Manual edit mode enabled.\nYou can modify the scene."
            )
        }
}