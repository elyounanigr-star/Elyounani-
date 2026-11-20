package com.example.device

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.euicc.EuiccManager

data class DeviceEuiccReport(
    val deviceModel: String,
    val androidVersion: String,
    val apiLevel: Int,
    val euiccHardwareAvailable: Boolean,
    val euiccOsVersion: String,
    val activeTransport: String,
    val isMetered: Boolean
)

object DeviceEuiccInspector {
    fun inspect(context: Context): DeviceEuiccReport {
        val euiccAvailable: Boolean
        val euiccOs: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val euiccManager = context.getSystemService(Context.EUICC_SERVICE) as? EuiccManager
            var enabled = false
            var osVersion = "Unavailable on virtual emulator"
            if (euiccManager != null) {
                try {
                    enabled = euiccManager.isEnabled
                    if (enabled) {
                        osVersion = euiccManager.euiccInfo?.osVersion ?: "SGP.22 Ready"
                    }
                } catch (_: SecurityException) {
                    enabled = false
                } catch (_: Exception) {
                    enabled = false
                }
            }
            euiccAvailable = enabled
            euiccOs = osVersion
        } else {
            euiccAvailable = false
            euiccOs = "Requires Android 9 (API 28)+"
        }

        val (transport, metered) = inspectActiveNetwork(context)

        return DeviceEuiccReport(
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT}",
            apiLevel = Build.VERSION.SDK_INT,
            euiccHardwareAvailable = euiccAvailable,
            euiccOsVersion = euiccOs,
            activeTransport = transport,
            isMetered = metered
        )
    }

    private fun inspectActiveNetwork(context: Context): Pair<String, Boolean> {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "Unknown" to false
            val network = cm.activeNetwork ?: return "Offline" to false
            val caps = cm.getNetworkCapabilities(network) ?: return "Offline" to false
            val transport = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Radio"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Tunnel"
                else -> "Connected"
            }
            val metered = cm.isActiveNetworkMetered
            transport to metered
        } catch (_: Exception) {
            "Unavailable" to false
        }
    }

    fun openSystemEsimSettings(context: Context): Boolean {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val euiccIntent = Intent(EuiccManager.ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (euiccIntent.resolveActivity(pm) != null) {
                return runCatching { context.startActivity(euiccIntent); true }.getOrDefault(false)
            }
        }
        val wirelessIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (wirelessIntent.resolveActivity(pm) != null) {
            return runCatching { context.startActivity(wirelessIntent); true }.getOrDefault(false)
        }
        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(fallback); true }.getOrDefault(false)
    }
}
