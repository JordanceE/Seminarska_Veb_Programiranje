export function showPage(pageId, tabId) {
    for (const id of ["app", "savedScenesPage", "sceneDetailsPage", "savedLocationsPage", "addLocationPage"]) {
        const page = document.getElementById(id)
        if (page) {
            const wasHidden = page.hidden
            page.hidden = id !== pageId
            if (wasHidden && !page.hidden) page.scrollTop = 0
        }
    }
    document.querySelectorAll("#mainTabs .tabButton").forEach(tab => {
        tab.classList.toggle("active", tab.id === tabId)
        tab.setAttribute("aria-current", tab.id === tabId ? "page" : "false")
    })
    window.dispatchEvent(new Event("resize"))
}
