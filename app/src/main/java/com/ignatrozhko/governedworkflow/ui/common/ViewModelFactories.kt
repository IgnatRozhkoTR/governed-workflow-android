package com.ignatrozhko.governedworkflow.ui.common

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.ignatrozhko.governedworkflow.AppContainer
import com.ignatrozhko.governedworkflow.GovernedWorkflowApp

val CreationExtras.appContainer: AppContainer
    get() = (checkNotNull(this[APPLICATION_KEY]) as GovernedWorkflowApp).container
