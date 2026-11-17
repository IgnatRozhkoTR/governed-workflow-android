package com.ignatrozhko.governedworkflow

import android.app.Application

class GovernedWorkflowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
