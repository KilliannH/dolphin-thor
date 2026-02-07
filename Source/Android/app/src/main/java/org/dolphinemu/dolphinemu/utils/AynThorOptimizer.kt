package org.dolphinemu.dolphinemu.utils

import android.content.Context
import org.dolphinemu.dolphinemu.features.settings.model.NativeConfig

object AynThorOptimizer {

    private const val PREF_KEY_OPTIMIZED = "ayn_thor_optimized"
    private const val TAG = "AynThorOptimizer"

    fun isAynThor(): Boolean {
        val soc = android.os.Build.SOC_MODEL ?: ""
        val device = android.os.Build.DEVICE ?: ""

        val isSD8Gen2 = soc.contains("SM8550", ignoreCase = true) ||
            soc.contains("kalama", ignoreCase = true)

        val isAynDevice = device.contains("thor", ignoreCase = true)

        return isSD8Gen2 || isAynDevice
    }

    fun applyOptimizationsIfNeeded(context: Context): Boolean {
        val prefs = context.getSharedPreferences("dolphin_ayn_thor", Context.MODE_PRIVATE)
        val alreadyOptimized = prefs.getBoolean(PREF_KEY_OPTIMIZED, false)

        if (alreadyOptimized) {
            Log.debug("[$TAG] Optimizations already applied")
            return false
        }

        if (!isAynThor()) {
            Log.debug("[$TAG] Not an Ayn Thor device")
            return false
        }

        Log.info("[$TAG] Ayn Thor detected! Applying optimizations...")

        return try {
            applyAynThorDefaults()
            prefs.edit().putBoolean(PREF_KEY_OPTIMIZED, true).apply()
            Log.info("[$TAG] Optimizations applied successfully!")
            true
        } catch (e: Exception) {
            Log.error("[$TAG] Failed to apply optimizations: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun applyAynThorDefaults() {
        val config = NativeConfig

        // ===== DOLPHIN.INI - CORE SETTINGS =====
        config.setBoolean(NativeConfig.LAYER_BASE, "Dolphin", "Core", "CPUThread", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "Dolphin", "Core", "SyncGPU", false)
        config.setInt(NativeConfig.LAYER_BASE, "Dolphin", "Core", "SyncGPUMaxDistance", 200000)
        config.setBoolean(NativeConfig.LAYER_BASE, "Dolphin", "Core", "FastDiscSpeed", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "Dolphin", "Core", "DSPHLE", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "Dolphin", "Core", "AudioStretch", false)

        // Backend Vulkan
        config.setString(NativeConfig.LAYER_BASE, "Dolphin", "Core", "GFXBackend", "Vulkan")

        // ===== DOLPHIN.INI - DSP =====
        config.setBoolean(NativeConfig.LAYER_BASE, "Dolphin", "DSP", "EnableJIT", true)
        config.setString(NativeConfig.LAYER_BASE, "Dolphin", "DSP", "Backend", "Cubeb")
        config.setInt(NativeConfig.LAYER_BASE, "Dolphin", "DSP", "Volume", 100)

        // ===== GFX.INI - SETTINGS =====
        // Shader compilation
        config.setInt(NativeConfig.LAYER_BASE, "GFX", "Settings", "ShaderCompilationMode", 2)
        config.setBoolean(
            NativeConfig.LAYER_BASE,
            "GFX",
            "Settings",
            "WaitForShadersBeforeStarting",
            false
        )

        // Resolution
        config.setInt(NativeConfig.LAYER_BASE, "GFX", "Settings", "InternalResolution", 3)

        // Progressive scan
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Settings", "ProgressiveScan", true)

        // VSync OFF
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Settings", "VSync", false)

        // ===== GFX.INI - ENHANCEMENTS =====
        // Anisotropic filtering
        config.setInt(NativeConfig.LAYER_BASE, "GFX", "Enhancements", "MaxAnisotropy", 4)

        // ===== GFX.INI - HACKS =====
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Hacks", "EFBAccessEnable", false)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Hacks", "EFBToTextureEnable", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Hacks", "EFBScaledCopy", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Hacks", "DeferEFBCopies", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Hacks", "SkipDuplicateXFBs", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Hacks", "ImmediateXFBEnable", false)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Hacks", "FastDepthCalc", true)
        config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "Hacks", "BBoxEnable", false)

        config.save(0)

        Log.info("[$TAG] Configuration saved to INI files")
    }

    fun getDeviceInfo(): String {
        return buildString {
            appendLine("=== Device Information ===")
            appendLine("Manufacturer: ${android.os.Build.MANUFACTURER}")
            appendLine("Model: ${android.os.Build.MODEL}")
            appendLine("Device: ${android.os.Build.DEVICE}")
            appendLine("SOC: ${android.os.Build.SOC_MODEL ?: "Unknown"}")
            appendLine("Is Ayn Thor: ${isAynThor()}")
            appendLine("========================")
        }
    }
}
