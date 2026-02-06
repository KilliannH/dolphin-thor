// SPDX-License-Identifier: GPL-2.0-or-later
package org.dolphinemu.dolphinemu.features.performance

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import org.dolphinemu.dolphinemu.features.settings.model.NativeConfig
import org.dolphinemu.dolphinemu.utils.Log

/**
 * Gestionnaire de performance pour Ayn Thor
 * Gère les profils et l'adaptation thermique automatique
 */
class PerformanceManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("dolphin_performance", Context.MODE_PRIVATE)
    private val thermalMonitor = ThermalMonitor(context)
    private val handler = Handler(Looper.getMainLooper())

    private var currentProfile: PerformanceProfile = PerformanceProfile.DEFAULT
    private var isAutoThermalEnabled = true
    private var thermalCheckRunnable: Runnable? = null

    companion object {
        private const val TAG = "PerformanceManager"
        private const val PREF_PERFORMANCE_PROFILE = "performance_profile"
        private const val PREF_AUTO_THERMAL = "auto_thermal_management"
        private const val THERMAL_CHECK_INTERVAL_MS = 5000L // 5 secondes

        @Volatile
        private var instance: PerformanceManager? = null

        fun getInstance(context: Context): PerformanceManager {
            return instance ?: synchronized(this) {
                instance ?: PerformanceManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    init {
        loadSettings()
        Log.info("[$TAG] Initialized with profile: ${currentProfile.name}")
    }

    /**
     * Charge les paramètres sauvegardés
     */
    private fun loadSettings() {
        val profileName = prefs.getString(PREF_PERFORMANCE_PROFILE, PerformanceProfile.DEFAULT.name)
        currentProfile = PerformanceProfile.fromString(profileName)
        isAutoThermalEnabled = prefs.getBoolean(PREF_AUTO_THERMAL, true)
    }

    /**
     * Change le profil de performance
     */
    fun setProfile(profile: PerformanceProfile) {
        Log.info("[$TAG] Switching profile: ${currentProfile.name} -> ${profile.name}")
        currentProfile = profile

        // Sauvegarder la préférence
        prefs.edit().putString(PREF_PERFORMANCE_PROFILE, profile.name).apply()

        // Appliquer le profil
        applyCurrentProfile()
    }

    /**
     * Récupère le profil actuel
     */
    fun getCurrentProfile(): PerformanceProfile = currentProfile

    /**
     * Active/désactive la gestion thermique automatique
     */
    fun setAutoThermalManagement(enabled: Boolean) {
        isAutoThermalEnabled = enabled
        prefs.edit().putBoolean(PREF_AUTO_THERMAL, enabled).apply()

        if (enabled) {
            startThermalMonitoring()
        } else {
            stopThermalMonitoring()
        }

        Log.info("[$TAG] Auto thermal management: $enabled")
    }

    /**
     * Vérifie si la gestion thermique auto est activée
     */
    fun isAutoThermalEnabled(): Boolean = isAutoThermalEnabled

    /**
     * Applique le profil actuel
     */
    fun applyCurrentProfile() {
        try {
            val config = NativeConfig()
            currentProfile.apply(config)
            config.saveSettings()

            Log.info("[$TAG] Profile ${currentProfile.name} applied successfully")
        } catch (e: Exception) {
            Log.error("[$TAG] Failed to apply profile: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Démarre le monitoring thermique
     */
    fun startThermalMonitoring() {
        if (!isAutoThermalEnabled) return

        stopThermalMonitoring() // Arrêter si déjà en cours

        thermalCheckRunnable = object : Runnable {
            override fun run() {
                checkThermalState()
                handler.postDelayed(this, THERMAL_CHECK_INTERVAL_MS)
            }
        }

        handler.post(thermalCheckRunnable!!)
        Log.info("[$TAG] Thermal monitoring started")
    }

    /**
     * Arrête le monitoring thermique
     */
    fun stopThermalMonitoring() {
        thermalCheckRunnable?.let {
            handler.removeCallbacks(it)
            thermalCheckRunnable = null
        }
        Log.info("[$TAG] Thermal monitoring stopped")
    }

    /**
     * Vérifie l'état thermique et ajuste si nécessaire
     */
    private fun checkThermalState() {
        val recommendedProfile = thermalMonitor.getRecommendedProfile(currentProfile)

        if (recommendedProfile != currentProfile) {
            Log.warning("[$TAG] Thermal adjustment: ${currentProfile.name} -> ${recommendedProfile.name}")

            // Appliquer temporairement le profil recommandé
            // Note: on ne change pas currentProfile pour garder la préférence utilisateur
            try {
                val config = NativeConfig()
                recommendedProfile.apply(config)
                config.saveSettings()
            } catch (e: Exception) {
                Log.error("[$TAG] Failed to apply thermal adjustment: ${e.message}")
            }
        }
    }

    /**
     * Récupère des statistiques de performance
     */
    fun getPerformanceStats(): String {
        return buildString {
            appendLine("=== Performance Stats ===")
            appendLine("Current Profile: ${currentProfile.profileName}")
            appendLine("Auto Thermal: $isAutoThermalEnabled")
            appendLine()
            append(thermalMonitor.getThermalInfo())
        }
    }

    /**
     * Force une vérification thermique immédiate
     */
    fun checkThermalNow() {
        checkThermalState()
    }
}
