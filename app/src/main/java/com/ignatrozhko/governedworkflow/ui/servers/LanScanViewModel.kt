package com.ignatrozhko.governedworkflow.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignatrozhko.governedworkflow.connection.NetworkMonitor
import com.ignatrozhko.governedworkflow.discovery.DiscoveredPanel
import com.ignatrozhko.governedworkflow.discovery.Ipv4Subnet
import com.ignatrozhko.governedworkflow.discovery.LanScanner
import com.ignatrozhko.governedworkflow.discovery.ScanEvent
import com.ignatrozhko.governedworkflow.ui.common.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LanScanUiState(
    val subnetLabel: String? = null,
    val probed: Int = 0,
    val total: Int = 0,
    val isScanning: Boolean = false,
    val found: List<DiscoveredPanel> = emptyList(),
    val error: String? = null,
)

class LanScanViewModel(private val networkMonitor: NetworkMonitor, private val scanner: LanScanner) : ViewModel() {
    private val _uiState = MutableStateFlow(LanScanUiState())
    val uiState: StateFlow<LanScanUiState> = _uiState.asStateFlow()
    private var scanJob: Job? = null

    init {
        scan()
    }

    fun scan() {
        val link = networkMonitor.localIpv4Link()
        if (link == null) {
            _uiState.value = LanScanUiState(error = "Connect to Wi-Fi to scan the local network.")
            return
        }
        val ownAddress = link.address.hostAddress.orEmpty()
        val subnet = Ipv4Subnet.scanRangeFor(link.address.address, link.prefixLength)
        val hosts = subnet.hostAddresses().filter { it != ownAddress }
        _uiState.value = LanScanUiState(
            subnetLabel = "${Ipv4Subnet.formatAddress(subnet.networkAddress)}/${subnet.prefixLength}",
            total = hosts.size,
            isScanning = true,
        )
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            scanner.scan(hosts).collect(::onScanEvent)
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    private fun onScanEvent(event: ScanEvent) = when (event) {
        is ScanEvent.Progress -> _uiState.update { it.copy(probed = event.probed, total = event.total) }
        is ScanEvent.Found -> _uiState.update { it.copy(found = it.found + event.panel) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { LanScanViewModel(appContainer.networkMonitor, appContainer.lanScanner) }
        }
    }
}
