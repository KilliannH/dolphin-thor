// SPDX-License-Identifier: GPL-2.0-or-later

package org.dolphinemu.dolphinemu.features.settings.model

import android.text.TextUtils
import org.dolphinemu.dolphinemu.NativeLibrary
import org.dolphinemu.dolphinemu.features.input.model.MappingCommon
import java.io.Closeable

/**
 * Represents a set of settings stored in the native part of Dolphin.
 *
 * A set of settings can be either the global settings, or game settings for a particular game.
 */
class Settings : Closeable {
    private var gameId: String = ""
    private var revision = 0

    var isWii = false
        private set

    private var settingsLoaded = false

    private val isGameSpecific: Boolean
        get() = !TextUtils.isEmpty(gameId)

    val writeLayer: Int
        get() = if (isGameSpecific) NativeConfig.LAYER_LOCAL_GAME else NativeConfig.LAYER_BASE_OR_CURRENT

    fun areSettingsLoaded(): Boolean {
        return settingsLoaded
    }

    @JvmOverloads
    fun loadSettings(isWii: Boolean = true) {
        this.isWii = isWii
        settingsLoaded = true

        if (isGameSpecific) {
            // Loading game INIs while the core is running will mess with the game INIs loaded by the core
            check(NativeLibrary.IsUninitialized()) { "Attempted to load game INI while emulating" }
            NativeConfig.loadGameInis(gameId, revision)
        }
    }

    fun loadSettings(gameId: String, revision: Int, isWii: Boolean) {
        this.gameId = gameId
        this.revision = revision
        loadSettings(isWii)
    }

    fun saveSettings() {
        if (!isGameSpecific) {
            MappingCommon.save()

            NativeConfig.save(NativeConfig.LAYER_BASE)

            NativeLibrary.ReloadLoggerConfig()
            NativeLibrary.UpdateGCAdapterScanThread()
        } else {
            NativeConfig.save(NativeConfig.LAYER_LOCAL_GAME)
        }
    }

    fun clearGameSettings() {
        NativeConfig.deleteAllKeys(NativeConfig.LAYER_LOCAL_GAME)
    }

    fun gameIniContainsJunk(): Boolean {
        // Older versions of Android Dolphin would copy the entire contents of most of the global INIs
        // into any game INI that got saved (with some of the sections renamed to match the game INI
        // section names). The problems with this are twofold:
        //
        // 1. The user game INIs will contain entries that Dolphin doesn't support reading from
        //    game INIs. This is annoying when editing game INIs manually but shouldn't really be
        //    a problem for those who only use the GUI.
        //
        // 2. Global settings will stick around in user game INIs. For instance, if someone wants to
        //    change the texture cache accuracy to safe for all games, they have to edit not only the
        //    global settings but also every single game INI they have created, since the old value of
        //    the texture cache accuracy Setting has been copied into every user game INI.
        //
        // These problems are serious enough that we should detect and delete such INI files.
        // Problem 1 is easy to detect, but due to the nature of problem 2, it's unfortunately not
        // possible to know which lines were added intentionally by the user and which lines were added
        // unintentionally, which is why we have to delete the whole file in order to fix everything.
        return if (!isGameSpecific) false else NativeConfig.exists(
            NativeConfig.LAYER_LOCAL_GAME,
            FILE_DOLPHIN,
            SECTION_INI_INTERFACE,
            "ThemeName"
        )
    }

    override fun close() {
        if (isGameSpecific) {
            NativeConfig.unloadGameInis()
        }
    }

    companion object {
        const val FILE_DOLPHIN = "Dolphin"
        const val FILE_SYSCONF = "SYSCONF"
        const val FILE_GFX = "GFX"
        const val FILE_LOGGER = "Logger"
        const val FILE_WIIMOTE = "WiimoteNew"
        const val FILE_ACHIEVEMENTS = "RetroAchievements"
        const val FILE_GAME_SETTINGS_ONLY = "GameSettingsOnly"
        const val SECTION_INI_ANDROID = "Android"
        const val SECTION_INI_ANDROID_OVERLAY_BUTTONS = "AndroidOverlayButtons"
        const val SECTION_INI_GENERAL = "General"
        const val SECTION_INI_CORE = "Core"
        const val SECTION_INI_INTERFACE = "Interface"
        const val SECTION_INI_DSP = "DSP"
        const val SECTION_LOGGER_LOGS = "Logs"
        const val SECTION_LOGGER_OPTIONS = "Options"
        const val SECTION_GFX_HARDWARE = "Hardware"
        const val SECTION_GFX_SETTINGS = "Settings"
        const val SECTION_GFX_ENHANCEMENTS = "Enhancements"
        const val SECTION_GFX_COLOR_CORRECTION = "ColorCorrection"
        const val SECTION_GFX_HACKS = "Hacks"
        const val SECTION_DEBUG = "Debug"
        const val SECTION_EMULATED_USB_DEVICES = "EmulatedUSBDevices"
        const val SECTION_STEREOSCOPY = "Stereoscopy"
        const val SECTION_ANALYTICS = "Analytics"
        const val SECTION_ACHIEVEMENTS = "Achievements"

        // AJOUT : Détection Ayn Thor / Snapdragon 8 Gen 2
        private fun isAynThor(): Boolean {
            val soc = android.os.Build.SOC_MODEL ?: ""
            val device = android.os.Build.DEVICE ?: ""
            val model = android.os.Build.MODEL ?: ""

            // Détection Snapdragon 8 Gen 2 (SM8550)
            return soc.contains("SM8550", ignoreCase = true) ||
                soc.contains("kalama", ignoreCase = true) ||
                device.contains("thor", ignoreCase = true) ||
                model.contains("thor", ignoreCase = true)
        }

        // AJOUT : Appliquer les optimisations Ayn Thor au premier lancement
        fun applyAynThorDefaults() {
            if (!isAynThor()) return

            val config = NativeConfig()

            // ===== CORE SETTINGS =====
            // CPU dual-core (essentiel pour performance)
            config.setBoolean(SECTION_INI_CORE, KEY_CPU_THREAD, true)

            // Sync GPU désactivé (meilleure perf sur SD8G2)
            config.setBoolean(SECTION_INI_CORE, KEY_SYNC_GPU, false)
            config.setInt(SECTION_INI_CORE, KEY_SYNC_GPU_MAX_DISTANCE, 200000)

            // Fast disc speed
            config.setBoolean(SECTION_INI_CORE, KEY_FAST_DISC_SPEED, true)

            // DSP HLE (High Level Emulation - plus rapide)
            config.setBoolean(SECTION_INI_DSP, KEY_DSP_HLE, true)
            config.setBoolean(SECTION_INI_DSP, KEY_DSP_JIT, true)

            // ===== GRAPHICS SETTINGS =====
            // Backend Vulkan (optimal pour Adreno 740)
            config.setString(SECTION_INI_CORE, KEY_GFX_BACKEND, "Vulkan")

            // Shader compilation asynchrone avec ubershaders hybride
            config.setInt(SECTION_GFX_SETTINGS, KEY_SHADER_COMPILATION_MODE, 2) // Async + Ubershaders
            config.setBoolean(SECTION_GFX_SETTINGS, KEY_WAIT_FOR_SHADERS, false)

            // Internal resolution 3x (balance perf/qualité pour Adreno 740)
            config.setInt(SECTION_GFX_SETTINGS, KEY_INTERNAL_RESOLUTION, 3)

            // Anisotropic filtering 4x
            config.setInt(SECTION_GFX_ENHANCEMENTS, KEY_ANISOTROPIC_FILTERING, 4)

            // Progressive scan
            config.setBoolean(SECTION_GFX_SETTINGS, KEY_PROGRESSIVE_SCAN, true)

            // ===== HACKS (Performance) =====
            // Skip EFB access from CPU (gros gain de perf)
            config.setBoolean(SECTION_GFX_HACKS, KEY_SKIP_EFB_CPU, true)

            // EFB to texture (au lieu de RAM)
            config.setBoolean(SECTION_GFX_HACKS, KEY_EFB_TEXTURE, true)
            config.setBoolean(SECTION_GFX_HACKS, KEY_EFB_SCALED_COPY, true)

            // Defer EFB copies
            config.setBoolean(SECTION_GFX_HACKS, KEY_DEFER_EFB_COPIES, true)

            // Skip duplicate XFBs
            config.setBoolean(SECTION_GFX_HACKS, KEY_SKIP_DUPLICATE_XFBS, true)

            // Immediate XFB désactivé (meilleure perf)
            config.setBoolean(SECTION_GFX_HACKS, KEY_IMMEDIATE_XFB, false)

            // Fast depth calculation (plus précis sur Adreno moderne)
            config.setBoolean(SECTION_GFX_HACKS, KEY_FAST_DEPTH_CALC, true)

            // Disable bounding box (sauf Paper Mario)
            config.setBoolean(SECTION_GFX_HACKS, KEY_BBOX_ENABLE, false)

            // VSync OFF pour meilleure perf
            config.setBoolean(SECTION_GFX_SETTINGS, KEY_VSYNC, false)

            // ===== AUDIO SETTINGS =====
            // Backend Cubeb (meilleur pour Android)
            config.setString(SECTION_INI_DSP, KEY_BACKEND, "Cubeb")

            // Volume par défaut
            config.setInt(SECTION_INI_DSP, KEY_VOLUME, 100)

            // Désactiver audio stretch (peut causer lag)
            config.setBoolean(SECTION_INI_CORE, KEY_AUDIO_STRETCH, false)

            // Sauvegarder
            config.saveSettings()

            android.util.Log.i("DolphinEmu", "Ayn Thor optimizations applied!")
        }
    }
}
