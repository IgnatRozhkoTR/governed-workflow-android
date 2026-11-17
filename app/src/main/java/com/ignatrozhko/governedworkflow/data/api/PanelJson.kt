package com.ignatrozhko.governedworkflow.data.api

import kotlinx.serialization.json.Json

/**
 * How the app reads and writes the panel's JSON. The panel is Python and sends `null` for empty optional
 * fields (`impact_analysis` before phase 1.3), so a null for a field with a default decodes as that default
 * instead of failing the whole response.
 */
val PanelJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}
