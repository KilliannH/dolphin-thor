// SPDX-License-Identifier: GPL-2.0-or-later
package org.dolphinemu.dolphinemu.features.performance

import org.dolphinemu.dolphinemu.features.settings.model.NativeConfig

/**
 * Profils de performance pour Ayn Thor (Snapdragon 8 Gen 2)
 */
enum class PerformanceProfile(
    val profileName: String,
    val description: String
) {
    BATTERY_SAVER(
        "Battery Saver",
        "Maximize battery life - Lower settings for extended gameplay"
    ) {
        override fun apply(config: NativeConfig) {
            // === CORE ===
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "CPUThread", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "SyncGPU", true) // Plus économe
            config.setInt(NativeConfig.LAYER_BASE, "Core", "SyncGPUMaxDistance", 100000)
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "FastDiscSpeed", true)

            // === GRAPHICS ===
            config.setString(NativeConfig.LAYER_BASE, "Core", "GFXBackend", "Vulkan")
            config.setInt(NativeConfig.LAYER_BASE, "GFX", "ShaderCompilationMode", 2)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "WaitForShadersBeforeStarting", false)

            // Résolution 2x (économie GPU)
            config.setInt(NativeConfig.LAYER_BASE, "GFX", "InternalResolution", 2)

            // Pas d'anisotropic filtering
            config.setInt(NativeConfig.LAYER_BASE, "GFX", "MaxAnisotropy", 0)

            // === HACKS (Performance Max) ===
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBAccessEnable", false)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBToTextureEnable", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBScaledCopy", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "DeferEFBCopies", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "SkipDuplicateXFBs", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "ImmediateXFBEnable", false)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "FastDepthCalc", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "BBoxEnable", false)

            // VSync pour limiter FPS et économiser batterie
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "VSync", true)

            // === AUDIO ===
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "DSPHLE", true)
            config.setString(NativeConfig.LAYER_BASE, "DSP", "Backend", "Cubeb")
        }
    },

    BALANCED(
        "Balanced",
        "Best balance between performance and battery - Recommended for most games"
    ) {
        override fun apply(config: NativeConfig) {
            // === CORE ===
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "CPUThread", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "SyncGPU", false)
            config.setInt(NativeConfig.LAYER_BASE, "Core", "SyncGPUMaxDistance", 200000)
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "FastDiscSpeed", true)

            // === GRAPHICS ===
            config.setString(NativeConfig.LAYER_BASE, "Core", "GFXBackend", "Vulkan")
            config.setInt(NativeConfig.LAYER_BASE, "GFX", "ShaderCompilationMode", 2)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "WaitForShadersBeforeStarting", false)

            // Résolution 3x (bon équilibre)
            config.setInt(NativeConfig.LAYER_BASE, "GFX", "InternalResolution", 3)

            // Anisotropic filtering 4x
            config.setInt(NativeConfig.LAYER_BASE, "GFX", "MaxAnisotropy", 4)

            // === HACKS ===
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBAccessEnable", false)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBToTextureEnable", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBScaledCopy", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "DeferEFBCopies", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "SkipDuplicateXFBs", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "ImmediateXFBEnable", false)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "FastDepthCalc", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "BBoxEnable", false)

            // VSync OFF
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "VSync", false)

            // === AUDIO ===
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "DSPHLE", true)
            config.setString(NativeConfig.LAYER_BASE, "DSP", "Backend", "Cubeb")
        }
    },

    PERFORMANCE(
        "Performance",
        "Maximum performance - Highest settings for demanding games"
    ) {
        override fun apply(config: NativeConfig) {
            // === CORE ===
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "CPUThread", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "SyncGPU", false)
            config.setInt(NativeConfig.LAYER_BASE, "Core", "SyncGPUMaxDistance", 400000)
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "FastDiscSpeed", true)

            // === GRAPHICS ===
            config.setString(NativeConfig.LAYER_BASE, "Core", "GFXBackend", "Vulkan")
            config.setInt(NativeConfig.LAYER_BASE, "GFX", "ShaderCompilationMode", 2)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "WaitForShadersBeforeStarting", false)

            // Résolution 4x (qualité maximale si SD8G2 peut gérer)
            config.setInt(NativeConfig.LAYER_BASE, "GFX", "InternalResolution", 4)

            // Anisotropic filtering 8x
            config.setInt(NativeConfig.LAYER_BASE, "GFX", "MaxAnisotropy", 8)

            // === HACKS ===
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBAccessEnable", false)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBToTextureEnable", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "EFBScaledCopy", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "DeferEFBCopies", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "SkipDuplicateXFBs", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "ImmediateXFBEnable", false)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "FastDepthCalc", true)
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "BBoxEnable", false)

            // VSync OFF
            config.setBoolean(NativeConfig.LAYER_BASE, "GFX", "VSync", false)

            // === AUDIO ===
            config.setBoolean(NativeConfig.LAYER_BASE, "Core", "DSPHLE", true)
            config.setString(NativeConfig.LAYER_BASE, "DSP", "Backend", "Cubeb")
        }
    };

    /**
     * Applique le profil de performance
     */
    abstract fun apply(config: NativeConfig)

    companion object {
        /**
         * Profil par défaut pour Ayn Thor
         */
        val DEFAULT = BALANCED

        /**
         * Récupère un profil depuis son nom
         */
        fun fromString(name: String?): PerformanceProfile {
            return values().find { it.name == name } ?: DEFAULT
        }
    }
}
