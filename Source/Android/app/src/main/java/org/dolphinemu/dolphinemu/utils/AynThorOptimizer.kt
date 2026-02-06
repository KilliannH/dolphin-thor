package org.dolphinemu.dolphinemu.utils

import android.content.Context
import androidx.preference.PreferenceManager
import org.dolphinemu.dolphinemu.features.settings.model.NativeConfig

object AynThorOptimizer {

    private const val PREF_KEY_OPTIMIZED = "ayn_thor_optimized"
    private const val TAG = "AynThorOptimizer"

    /**
     * Détecte si l'appareil est un Ayn Thor (Snapdragon 8 Gen 2)
     */
    fun isAynThor(): Boolean {
        val soc = android.os.Build.SOC_MODEL ?: ""
        val device = android.os.Build.DEVICE ?: ""
        val model = android.os.Build.MODEL ?: ""

        val isSD8Gen2 = soc.contains("SM8550", ignoreCase = true) ||
            soc.contains("kalama", ignoreCase = true)

        val isAynDevice = device.contains("thor", ignoreCase = true) ||
            model.contains("thor", ignoreCase = true) ||
            model.contains("AYN", ignoreCase = true)

        return isSD8Gen2 || isAynDevice
    }

    /**
     * Détecte le GPU Adreno 740
     */
    fun isAdreno740(): Boolean {
        return try {
            val activityManager = android.app.ActivityManager::class.java
            val configurationInfo = activityManager.getDeclaredMethod("getDeviceConfigurationInfo")
            // Détection via GL_RENDERER serait plus précise mais nécessite contexte GL
            // Pour l'instant on se base sur le SOC
            android.os.Build.SOC_MODEL?.contains("SM8550", ignoreCase = true) ?: false
        } catch (e: Exception) {
            Log.error("[$TAG] Failed to detect Adreno 740: ${e.message}")
            false
        }
    }

    /**
     * Applique les optimisations si nécessaire
     * @return true si les optimisations ont été appliquées, false sinon
     */
    fun applyOptimizationsIfNeeded(context: Context): Boolean {
        val prefs = context.getSharedPreferences("dolphin_performance", Context.MODE_PRIVATE)
        val alreadyOptimized = prefs.getBoolean(PREF_KEY_OPTIMIZED, false)

        if (alreadyOptimized) {
            Log.debug("[$TAG] Optimizations already applied")
            return false
        }

        if (!isAynThor()) {
            Log.debug("[$TAG] Not an Ayn Thor device, skipping optimizations")
            return false
        }

        Log.info("[$TAG] Ayn Thor detected! Applying optimizations...")

        return try {
            applyOptimizations()
            prefs.edit().putBoolean(PREF_KEY_OPTIMIZED, true).apply()
            Log.info("[$TAG] Optimizations applied successfully!")
            true
        } catch (e: Exception) {
            Log.error("[$TAG] Failed to apply optimizations: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Force la réapplication des optimisations (utile pour debug)
     */
    fun forceApplyOptimizations(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(PREF_KEY_OPTIMIZED, false).apply()
        return applyOptimizationsIfNeeded(context)
    }

    private fun applyOptimizations() {
        val config = NativeConfig()

        // Core settings
        config.setBoolean(NativeConfig.LAYER_BASE, "Core", "CPUThread", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "Core", "SyncGPU", false)
        config.setInt(NativeConfig.LAYER_BASE, "Core", "SyncGPUMaxDistance", 200000)
        config.setBoolean(NativeConfig.LAYER_BASE, "Core", "FastDiscSpeed", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "Core", "DSPHLE", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "Core", "AudioStretch", false)

        // DSP settings
        config.setBoolean(NativeConfig.LAYER_BASE, "DSP", "EnableJIT", true)
        config.setString(NativeConfig.LAYER_BASE, "DSP", "Backend", "Cubeb")
        config.setInt(NativeConfig.LAYER_BASE, "DSP", "Volume", 100)

        // Graphics backend
        config.setString(NativeConfig.LAYER_BASE, "Core", "GFXBackend", "Vulkan")

        // Shader compilation
        config.setInt(NativeConfig.LAYER_BASE, "GFX", "ShaderCompilationMode", 2)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "WaitForShadersBeforeStarting", false)

        // Resolution & filtering
        config.setInt(NativeConfig.LAYER_BASE, "GFX", "InternalResolution", 3)
        config.setInt(NativeConfig.LAYER_BASE, "GFX", "MaxAnisotropy", 4)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "ProgressiveScan", true)

        // Performance hacks
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBAccessEnable", false)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBToTextureEnable", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBScaledCopy", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "DeferEFBCopies", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "SkipDuplicateXFBs", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "ImmediateXFBEnable", false)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "FastDepthCalc", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "BBoxEnable", false)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "VSync", false)

        config.saveSettings()

        Log.info("[$TAG] Configuration saved")
    }

    /**
     * Retourne les informations du device pour debug
     */
    fun getDeviceInfo(): String {
        return buildString {
            appendLine("=== Device Information ===")
            appendLine("Manufacturer: ${android.os.Build.MANUFACTURER}")
            appendLine("Brand: ${android.os.Build.BRAND}")
            appendLine("Model: ${android.os.Build.MODEL}")
            appendLine("Device: ${android.os.Build.DEVICE}")
            appendLine("Product: ${android.os.Build.PRODUCT}")
            appendLine("SOC Model: ${android.os.Build.SOC_MODEL ?: "Unknown"}")
            appendLine("SOC Manufacturer: ${android.os.Build.SOC_MANUFACTURER ?: "Unknown"}")
            appendLine("Hardware: ${android.os.Build.HARDWARE}")
            appendLine("Is Ayn Thor: ${isAynThor()}")
            appendLine("Is Adreno 740: ${isAdreno740()}")
            appendLine("========================")
        }
    }
}
