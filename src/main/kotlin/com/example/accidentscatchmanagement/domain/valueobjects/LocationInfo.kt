package com.example.accidentscatchmanagement.domain.valueobjects

// Legacy payload type retained for compatibility with older scene exports/events.
// Current scenes persist their reusable location through the Location entity.
data class LocationInfo(
    var fileName: String? = null,
    var name: String? = null,
    var description: String? = null,

    var topRoadWidth: Double? = null,
    var topRoadLanes: Int? = null,

    var bottomRoadWidth: Double? = null,
    var bottomRoadLanes: Int? = null,

    var leftRoadWidth: Double? = null,
    var leftRoadLanes: Int? = null,

    var rightRoadWidth: Double? = null,
    var rightRoadLanes: Int? = null,

    var roundaboutDiameter: Double? = null,
    var tJunction: Boolean = false
)
