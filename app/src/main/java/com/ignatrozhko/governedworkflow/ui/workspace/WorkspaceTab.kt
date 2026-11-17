package com.ignatrozhko.governedworkflow.ui.workspace

import androidx.annotation.DrawableRes
import com.ignatrozhko.governedworkflow.R

/** Bottom-bar tabs of the workspace screen; the web panel's other tabs are sections inside Overview. */
enum class WorkspaceTab(val label: String, @param:DrawableRes val iconRes: Int) {
    Overview("Overview", R.drawable.ic_tab_overview),
    Changes("Changes", R.drawable.ic_tab_changes),
    Terminal("Terminal", R.drawable.ic_tab_terminal),
}
