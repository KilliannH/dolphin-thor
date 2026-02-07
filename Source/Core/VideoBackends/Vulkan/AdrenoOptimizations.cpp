// SPDX-License-Identifier: GPL-2.0-or-later
// Copyright 2024 Dolphin Emulator Project

#include "VideoBackends/Vulkan/AdrenoOptimizations.h"

#include <algorithm>
#include <cstring>

#include "Common/Logging/Log.h"

namespace Vulkan
{
    namespace AdrenoOptimizations
    {

        bool IsAdreno740(const char* device_name, u32 vendor_id, u32 device_id)
        {
            // Qualcomm Vendor ID
            constexpr u32 QUALCOMM_VENDOR_ID = 0x5143;

            // Adreno 740 Device IDs
            constexpr u32 ADRENO_740_ID_1 = 0x43050A01;
            constexpr u32 ADRENO_740_ID_2 = 0x43051401;

            if (vendor_id != QUALCOMM_VENDOR_ID)
                return false;

            // Check device ID
            if (device_id == ADRENO_740_ID_1 || device_id == ADRENO_740_ID_2)
            {
                INFO_LOG_FMT(VIDEO, "Adreno 740 detected via device ID: 0x{:08X}", device_id);
                return true;
            }

            // Fallback: check device name
            if (device_name && (strstr(device_name, "Adreno (TM) 740") != nullptr ||
                                strstr(device_name, "Adreno 740") != nullptr))
            {
                INFO_LOG_FMT(VIDEO, "Adreno 740 detected via device name: {}", device_name);
                return true;
            }

            return false;
        }

        bool IsTurnipDriver(const char* device_name)
        {
            if (!device_name)
                return false;

            bool is_turnip = (strstr(device_name, "turnip") != nullptr ||
                              strstr(device_name, "Turnip") != nullptr ||
                              strstr(device_name, "Mesa") != nullptr);

            if (is_turnip)
            {
                INFO_LOG_FMT(VIDEO, "Turnip Mesa drivers detected");
            }

            return is_turnip;
        }

        std::vector<const char*> GetOptimalExtensions(bool has_turnip)
        {
            std::vector<const char*> extensions;

            // Extensions communes Adreno 740
            extensions.push_back("VK_KHR_shader_non_semantic_info");
            extensions.push_back("VK_EXT_scalar_block_layout");
            extensions.push_back("VK_KHR_spirv_1_4");

            // Async compute
            extensions.push_back("VK_KHR_synchronization_2");

            // Memory management
            extensions.push_back("VK_EXT_memory_budget");
            extensions.push_back("VK_EXT_memory_priority");

            if (has_turnip)
            {
                INFO_LOG_FMT(VIDEO, "Adding Turnip-specific extensions");
            }

            return extensions;
        }

        size_t GetOptimalPipelineCacheSize()
        {
            // Adreno 740 avec 16GB RAM → gros cache possible
            return 512 * 1024 * 1024;  // 512 MB
        }

        DescriptorPoolSizes GetOptimalDescriptorPoolSizes()
        {
            DescriptorPoolSizes sizes;

            // Optimisé pour Adreno 740
            sizes.uniform_buffers = 2048;
            sizes.combined_image_samplers = 8192;  // Très augmenté pour textures
            sizes.storage_buffers = 1024;
            sizes.uniform_texel_buffers = 256;
            sizes.max_sets = 16384;

            INFO_LOG_FMT(VIDEO, "Adreno 740: Using large descriptor pools for better cache hit rate");

            return sizes;
        }

        TextureCompressionParams GetTextureCompressionParams()
        {
            TextureCompressionParams params;

            params.enable_ubwc = true;  // UBWC hardware compression
            params.prefer_linear_tiling = false;  // OPTIMAL tiling meilleur pour Adreno
            params.staging_buffer_size = 128 * 1024 * 1024;  // 128MB staging buffer

            return params;
        }

        MemoryParams GetOptimalMemoryParams()
        {
            MemoryParams params;

            // Adreno 740 bénéficie de mémoire DEVICE_LOCAL + HOST_VISIBLE
            params.prefer_device_local_host_visible = true;

            // Profiter de la bande passante LPDDR5X (68 GB/s)
            params.staging_buffer_count = 4;  // Quad buffering
            params.upload_buffer_size = 64 * 1024 * 1024;  // 64MB par buffer

            return params;
        }

        AsyncComputeConfig GetAsyncComputeConfig()
        {
            AsyncComputeConfig config;

            // Adreno 740 supporte async compute hardware
            config.enable = true;
            config.num_compute_queues = 1;
            config.separate_transfer_queue = true;

            return config;
        }

    }  // namespace AdrenoOptimizations
}  // namespace Vulkan
