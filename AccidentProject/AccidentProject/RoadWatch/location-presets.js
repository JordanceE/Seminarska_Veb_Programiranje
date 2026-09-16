// Example dimensions in metres, editable before saving. Missing road arms stay blank.
const roads = (directions, lanes = 2, width = 7) => Object.fromEntries(
    directions.flatMap(direction => [[direction + "RoadWidth", width], [direction + "RoadLanes", lanes]])
)
const all = ["top", "bottom", "left", "right"]
const three = ["top", "left", "right"]

export const LOCATION_PRESETS = {
    "glavnaulica.png": roads(all),
    "roundabout-3-exits.png": { ...roads(three, 1, 3.5), roundaboutDiameter: 20 },
    "roundabout-3-exits-2-lanes.png": { ...roads(three, 2, 7), roundaboutDiameter: 30 },
    "roundabout-4-way-1-lanes.png": { ...roads(all, 1, 3.5), roundaboutDiameter: 20 },
    "roundabout-4-way-2-lanes.png": { ...roads(all, 2, 7), roundaboutDiameter: 30 },
    // The supplied T-junction picture has a bottom arm and no top arm.
    "t-junction.png": { ...roads(["bottom", "left", "right"]), tJunction: true },
    "boulevard_2_full_lines.png": roads(["top", "bottom"], 6, 21),
    "boulevard_tree_line.png": roads(["top", "bottom"], 6, 21)
}
