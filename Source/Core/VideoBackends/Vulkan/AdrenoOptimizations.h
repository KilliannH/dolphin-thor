// SPDX-License-Identifier: GPL-2.0-or-later
// Copyright 2024 Dolphin Emulator Project

#pragma once

#include <string>
#include <vector>
#include "Common/CommonTypes.h"

namespace Vulkan
{
    namespace AdrenoOptimizations
    {

/**
 * Détecte si le GPU est un Adreno 740
 */
        bool IsAdreno740(const char* device_name, u32 vendor_id, u32 device_id);

/**
 * Détecte si on utilise les drivers Turnip (Mesa)
 */
        bool IsTurnipDriver(const char* device_name);

/**
 * Récupère les extensions Vulkan optimales pour Adreno 740
 */
        std::vector<const char*> GetOptimalExtensions(bool has_turnip);

/**
 * Taille optimale du pipeline cache pour Adreno 740
 */
        size_t GetOptimalPipelineCacheSize();

/**
 * Taille optimale des descriptor pools
 */
        struct DescriptorPoolSizes
        {
            u32 uniform_buffers;
            u32 combined_image_samplers;
            u32 storage_buffers;
            u32 uniform_texel_buffers;
            u32 max_sets;
        };

        DescriptorPoolSizes GetOptimalDescriptorPoolSizes();

/**
 * Paramètres de compression texture optimaux
 */
        struct TextureCompressionParams
        {
            bool enable_ubwc;
            bool prefer_linear_tiling;
            u32 staging_buffer_size;
        };

        TextureCompressionParams GetTextureCompressionParams();

/**
 * Paramètres de mémoire optimaux
 */
        struct MemoryParams
        {
            bool prefer_device_local_host_visible;
            u32 staging_buffer_count;
            u32 upload_buffer_size;
        };

        MemoryParams GetOptimalMemoryParams();

/**
 * Configuration async compute
 */
        struct AsyncComputeConfig
        {
            bool enable;
            u32 num_compute_queues;
            bool separate_transfer_queue;
        };

        AsyncComputeConfig GetAsyncComputeConfig();

    }  // namespace AdrenoOptimizations
}  // namespace Vulkan
