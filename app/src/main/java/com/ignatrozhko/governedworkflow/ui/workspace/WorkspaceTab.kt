package com.ignatrozhko.governedworkflow.ui.workspace

import androidx.annotation.DrawableRes
import com.ignatrozhko.governedworkflow.R

/** Tabs of the workspace screen; later rounds add entries here and their content branch in [WorkspaceScreen]. */
enum class WorkspaceTab(val label: String, @param:DrawableRes val iconRes: Int) {
    Overview("Overview", R.drawable.ic_tab_overview),
    Terminal("Terminal", R.drawable.ic_tab_terminal),
}
