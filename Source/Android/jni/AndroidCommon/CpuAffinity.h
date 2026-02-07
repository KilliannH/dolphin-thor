// SPDX-License-Identifier: GPL-2.0-or-later

#pragma once

namespace AndroidCpuAffinity
{
/**
 * Initialize CPU topology detection
 * Call this once at app startup
 */
    void Initialize();

/**
 * Check if device is Snapdragon 8 Gen 2
 */
    bool IsSnapdragon8Gen2();

/**
 * Set CPU affinity for PowerPC emulation thread
 * Pins to Gold cores (Cortex-A715)
 */
    void SetPowerPCThreadAffinity();

/**
 * Set CPU affinity for GPU thread
 * Pins to Prime + Gold cores
 */
    void SetGPUThreadAffinity();

/**
 * Set CPU affinity for Audio thread
 * Pins to Silver cores (Cortex-A510)
 */
    void SetAudioThreadAffinity();

/**
 * Set CPU affinity for generic threads
 * No pinning - let scheduler decide
 */
    void SetGenericThreadAffinity();

/**
 * Request performance CPU governor
 * Note: May require root on some devices
 */
    void SetCpuGovernorPerformance();

/**
 * Get recommended thread count for work pools
 */
    int GetRecommendedThreadCount();

}  // namespace AndroidCpuAffinity
