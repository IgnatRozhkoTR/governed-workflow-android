package com.ignatrozhko.governedworkflow.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import java.net.Inet4Address

class NetworkMonitor(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    /** Emits once per settled change of the default network (switching Wi-Fi, cellular, VPN, address). */
    @OptIn(FlowPreview::class)
    val changes: Flow<Unit> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(Unit)
            }

            override fun onLost(network: Network) {
                trySend(Unit)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                trySend(Unit)
            }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.debounce(SETTLE_MS)

    /** The IPv4 address of the active Wi-Fi or Ethernet link, used to derive the LAN to scan. */
    fun localIpv4Link(): LinkAddress? {
        val network = connectivity.activeNetwork ?: return null
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return null
        val isLocalLink = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        if (!isLocalLink) return null
        return connectivity.getLinkProperties(network)?.linkAddresses?.firstOrNull { it.address is Inet4Address }
    }

    private companion object {
        const val SETTLE_MS = 600L
    }
}
