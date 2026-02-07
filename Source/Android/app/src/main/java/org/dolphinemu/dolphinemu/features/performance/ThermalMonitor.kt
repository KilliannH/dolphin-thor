// SPDX-License-Identifier: GPL-2.0-or-later
package org.dolphinemu.dolphinemu.features.performance

import android.content.Context
import android.os.Build
import android.os.PowerManager
import org.dolphinemu.dolphinemu.utils.Log

/**
 * Moniteur thermique pour Ayn Thor
 * Surveille la température et ajuste les performances automatiquement
 */
class ThermalMonitor(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    /**
     * États thermiques Android
     */
    enum class ThermalState(val value: Int) {
        NONE(0),           // Température normale
        LIGHT(1),          // Légère chaleur
        MODERATE(2),       // Chaleur modérée
        SEVERE(3),         // Chaleur sévère - throttling recommandé
        CRITICAL(4),       // Critique - réduction drastique nécessaire
        EMERGENCY(5),      // Urgence - arrêt recommandé
        SHUTDOWN(6);       // Arrêt imminent

        companion object {
            fun fromInt(value: Int) = values().find { it.value == value } ?: NONE
        }
    }

    /**
     * Récupère l'état thermique actuel (Android 9+)
     */
    fun getCurrentThermalState(context: Context): ThermalState {
        if (Build.VERSION.SDK_INT < 29) return ThermalState.NONE

        return try {
            // Pas besoin de Context.THERMAL_SERVICE
            val svc = context.getSystemService("thermal") ?: return ThermalState.NONE

            val tmClass = Class.forName("android.os.ThermalManager")
            if (!tmClass.isInstance(svc)) return ThermalState.NONE

            // Méthode Java: getCurrentThermalStatus()
            val method = tmClass.getMethod("getCurrentThermalStatus")
            val status = method.invoke(svc) as Int

            ThermalState.fromInt(status)
        } catch (_: Throwable) {
            ThermalState.NONE
        }
    }

    /**
     * Récupère l'état thermique (compatible Android 5+)
     */
    fun getThermalStateLegacy(): ThermalState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getCurrentThermalState(context)
        } else {
            // Fallback pour Android < 9 : utiliser battery temperature
            estimateThermalFromBattery()
        }
    }

    /**
     * Estime l'état thermique depuis la température batterie
     */
    private fun estimateThermalFromBattery(): ThermalState {
        return try {
            val batteryTemp = getBatteryTemperature()
            when {
                batteryTemp < 35 -> ThermalState.NONE
                batteryTemp < 40 -> ThermalState.LIGHT
                batteryTemp < 45 -> ThermalState.MODERATE
                batteryTemp < 50 -> ThermalState.SEVERE
                batteryTemp < 55 -> ThermalState.CRITICAL
                else -> ThermalState.EMERGENCY
            }
        } catch (e: Exception) {
            Log.error("[ThermalMonitor] Failed to estimate thermal state: ${e.message}")
            ThermalState.NONE
        }
    }

    /**
     * Récupère la température de la batterie en °C
     */
    private fun getBatteryTemperature(): Float {
        val intent = context.registerReceiver(
            null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        )
        val temp = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return temp / 10f // Convertir de dixièmes de degrés en degrés
    }

    /**
     * Vérifie si le throttling thermique est recommandé
     */
    fun shouldThrottle(): Boolean {
        val state = getThermalStateLegacy()
        return state.value >= ThermalState.SEVERE.value
    }

    /**
     * Récupère le profil recommandé selon l'état thermique
     */
    fun getRecommendedProfile(currentProfile: PerformanceProfile): PerformanceProfile {
        val thermalState = getThermalStateLegacy()

        return when {
            thermalState.value >= ThermalState.CRITICAL.value -> {
                Log.warning("[ThermalMonitor] Critical temperature! Forcing Battery Saver mode")
                PerformanceProfile.BATTERY_SAVER
            }
            thermalState.value >= ThermalState.SEVERE.value -> {
                // Descendre d'un niveau
                when (currentProfile) {
                    PerformanceProfile.PERFORMANCE -> {
                        Log.info("[ThermalMonitor] Severe temperature, downgrading to Balanced")
                        PerformanceProfile.BALANCED
                    }
                    PerformanceProfile.BALANCED -> {
                        Log.info("[ThermalMonitor] Severe temperature, downgrading to Battery Saver")
                        PerformanceProfile.BATTERY_SAVER
                    }
                    else -> currentProfile
                }
            }
            else -> currentProfile
        }
    }

    /**
     * Retourne des informations thermiques détaillées
     */
    fun getThermalInfo(): String {
        val state = getThermalStateLegacy()
        val batteryTemp = getBatteryTemperature()
        val isPowerSaveMode = powerManager?.isPowerSaveMode ?: false

        return buildString {
            appendLine("=== Thermal Status ===")
            appendLine("State: ${state.name} (${state.value})")
            appendLine("Battery Temp: ${String.format("%.1f", batteryTemp)}°C")
            appendLine("Power Save Mode: $isPowerSaveMode")
            appendLine("Should Throttle: ${shouldThrottle()}")
            appendLine("====================")
        }
    }
}
