package com.monitortv.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.monitortv.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

class TvDiscoveryManager(context: Context) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val executor = Executor { it.run() }
    private val found = ConcurrentHashMap<String, TvDevice>()
    private val _devices = MutableStateFlow<List<TvDevice>>(emptyList())
    val devices: StateFlow<List<TvDevice>> = _devices.asStateFlow()
    private val _status = MutableStateFlow("Ready to scan")
    val status: StateFlow<String> = _status.asStateFlow()
    private var discovering = false
    private val serviceType = "_androidtvremote2._tcp."
    private val callbacks = ConcurrentHashMap<String, NsdManager.ServiceInfoCallback>()

    private val listener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) { _status.value = "Searching for Android TV devices…" }
        override fun onServiceFound(info: NsdServiceInfo) {
            if (!info.serviceType.contains("androidtvremote2")) return
            if (Build.VERSION.SDK_INT >= 34) {
                val callback = object : NsdManager.ServiceInfoCallback {
                    override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                        _status.value = "Could not resolve ${info.serviceName}: $errorCode"
                    }
                    override fun onServiceUpdated(resolved: NsdServiceInfo) {
                        found[resolved.serviceName] = TvDevice(resolved.serviceName, resolved.hostAddresses.firstOrNull(), resolved.port, resolved.serviceType)
                        publish()
                    }
                    override fun onServiceLost() { found.remove(info.serviceName); publish() }
                    override fun onServiceInfoCallbackUnregistered() { callbacks.remove(info.serviceName) }
                }
                callbacks[info.serviceName] = callback
                nsd.registerServiceInfoCallback(info, executor, callback)
            } else {
                @Suppress("DEPRECATION")
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        _status.value = "Could not resolve ${serviceInfo.serviceName}: $errorCode"
                    }
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        found[resolved.serviceName] = TvDevice(resolved.serviceName, resolved.host, resolved.port, resolved.serviceType)
                        publish()
                    }
                })
            }
        }
        override fun onServiceLost(info: NsdServiceInfo) { found.remove(info.serviceName); publish() }
        override fun onStartDiscoveryFailed(type: String, errorCode: Int) {
            discovering = false
            _status.value = "Discovery failed ($errorCode). Check Wi-Fi and permission."
            runCatching { nsd.stopServiceDiscovery(this) }
        }
        override fun onStopDiscoveryFailed(type: String, errorCode: Int) {
            _status.value = "Could not stop discovery ($errorCode)"
            runCatching { nsd.stopServiceDiscovery(this) }
        }
        override fun onDiscoveryStopped(type: String) { discovering = false; _status.value = "Scan stopped" }
    }

    fun start() {
        if (discovering) return
        found.clear(); publish()
        try {
            discovering = true
            nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: SecurityException) {
            discovering = false
            _status.value = "Local-network permission is required."
        } catch (e: Exception) {
            discovering = false
            _status.value = "Unable to scan: ${e.message ?: "unknown error"}"
        }
    }

    fun stop() {
        if (discovering) runCatching { nsd.stopServiceDiscovery(listener) }
        if (Build.VERSION.SDK_INT >= 34) callbacks.values.forEach { runCatching { nsd.unregisterServiceInfoCallback(it) } }
        callbacks.clear()
        discovering = false
    }

    private fun publish() {
        _devices.value = found.values.sortedBy { it.displayName.lowercase() }
        if (found.isNotEmpty()) _status.value = "${found.size} compatible TV remote service(s) found"
    }
}
