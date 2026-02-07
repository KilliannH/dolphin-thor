// SPDX-License-Identifier: GPL-2.0-or-later

#include "jni/AndroidCommon/CpuAffinity.h"

#include <pthread.h>
#include <sched.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <vector>

#include "Common/CommonTypes.h"
#include "Common/Logging/Log.h"

#ifdef __ANDROID__
#include <sys/system_properties.h>
#endif

namespace AndroidCpuAffinity
{
    namespace
    {
        struct CpuTopology
        {
            bool is_snapdragon_8_gen_2 = false;
            int prime_core = -1;          // Cortex-X3 (core 0)
            int gold_cores_start = -1;    // Cortex-A715 (cores 1-4)
            int gold_cores_end = -1;
            int silver_cores_start = -1;  // Cortex-A510 (cores 5-7)
            int silver_cores_end = -1;
            int total_cores = 0;
        };

        CpuTopology g_topology;

        bool DetectSnapdragon8Gen2()
        {
#ifdef __ANDROID__
            char soc_model[PROP_VALUE_MAX];
  char soc_manufacturer[PROP_VALUE_MAX];

  __system_property_get("ro.soc.model", soc_model);
  __system_property_get("ro.soc.manufacturer", soc_manufacturer);

  // Snapdragon 8 Gen 2 = SM8550 = "kalama"
  bool is_sm8550 = (strstr(soc_model, "SM8550") != nullptr ||
                    strstr(soc_model, "kalama") != nullptr);
  bool is_qualcomm = (strstr(soc_manufacturer, "Qualcomm") != nullptr ||
                      strstr(soc_manufacturer, "QTI") != nullptr);

  if (is_sm8550 && is_qualcomm)
  {
    INFO_LOG_FMT(COMMON, "Detected Snapdragon 8 Gen 2 (SM8550)");
    return true;
  }
#endif
            return false;
        }

        void InitializeCpuTopology()
        {
            g_topology.total_cores = (int)sysconf(_SC_NPROCESSORS_CONF);

            if (DetectSnapdragon8Gen2())
            {
                g_topology.is_snapdragon_8_gen_2 = true;

                // Topologie Snapdragon 8 Gen 2
                // Core 0: X3 (Prime)
                // Cores 1-4: A715 (Gold)
                // Cores 5-7: A510 (Silver)
                g_topology.prime_core = 0;
                g_topology.gold_cores_start = 1;
                g_topology.gold_cores_end = 4;
                g_topology.silver_cores_start = 5;
                g_topology.silver_cores_end = 7;

                INFO_LOG_FMT(COMMON,
                             "CPU Topology: Prime={}, Gold={}-{}, Silver={}-{}, Total={}",
                             g_topology.prime_core,
                             g_topology.gold_cores_start, g_topology.gold_cores_end,
                             g_topology.silver_cores_start, g_topology.silver_cores_end,
                             g_topology.total_cores);
            }
            else
            {
                WARN_LOG_FMT(COMMON, "Not a Snapdragon 8 Gen 2 device, CPU affinity disabled");
            }
        }

        bool SetThreadAffinityToCores(const std::vector<int>& cores)
        {
            if (cores.empty())
                return false;

            cpu_set_t cpuset;
            CPU_ZERO(&cpuset);

            for (int core : cores)
            {
                if (core >= 0 && core < g_topology.total_cores)
                {
                    CPU_SET(core, &cpuset);
                }
            }

            pthread_t current_thread = pthread_self();
            int result = sched_setaffinity(current_thread, sizeof(cpu_set_t), &cpuset);

            if (result != 0)
            {
                ERROR_LOG_FMT(COMMON, "Failed to set CPU affinity: {}", strerror(errno));
                return false;
            }

            // Log cores assignés
            std::string core_list;
            for (size_t i = 0; i < cores.size(); i++)
            {
                core_list += std::to_string(cores[i]);
                if (i < cores.size() - 1)
                    core_list += ",";
            }

            INFO_LOG_FMT(COMMON, "Thread {} affinity set to cores: {}",
                         syscall(SYS_gettid), core_list);

            return true;
        }

    }  // namespace

    void Initialize()
    {
        InitializeCpuTopology();
    }

    bool IsSnapdragon8Gen2()
    {
        return g_topology.is_snapdragon_8_gen_2;
    }

    void SetPowerPCThreadAffinity()
    {
        if (!g_topology.is_snapdragon_8_gen_2)
            return;

        // PowerPC thread → Gold cores (1-4)
        // Besoin de haute perf sustained
        std::vector<int> gold_cores;
        for (int i = g_topology.gold_cores_start; i <= g_topology.gold_cores_end; i++)
        {
            gold_cores.push_back(i);
        }

        if (SetThreadAffinityToCores(gold_cores))
        {
            INFO_LOG_FMT(COMMON, "PowerPC thread pinned to Gold cores");
        }
    }

    void SetGPUThreadAffinity()
    {
        if (!g_topology.is_snapdragon_8_gen_2)
            return;

        // GPU thread → Prime + Gold cores (0-4)
        // Besoin de pics de performance
        std::vector<int> perf_cores;
        perf_cores.push_back(g_topology.prime_core);
        for (int i = g_topology.gold_cores_start; i <= g_topology.gold_cores_end; i++)
        {
            perf_cores.push_back(i);
        }

        if (SetThreadAffinityToCores(perf_cores))
        {
            INFO_LOG_FMT(COMMON, "GPU thread pinned to Prime+Gold cores");
        }
    }

    void SetAudioThreadAffinity()
    {
        if (!g_topology.is_snapdragon_8_gen_2)
            return;

        // Audio thread → Silver cores (5-7)
        // Pas besoin de haute performance
        std::vector<int> silver_cores;
        for (int i = g_topology.silver_cores_start; i <= g_topology.silver_cores_end; i++)
        {
            silver_cores.push_back(i);
        }

        if (SetThreadAffinityToCores(silver_cores))
        {
            INFO_LOG_FMT(COMMON, "Audio thread pinned to Silver cores");
        }
    }

    void SetGenericThreadAffinity()
    {
        // Laisser le scheduler décider pour les threads génériques
        // Pas d'affinité forcée
    }

    void SetCpuGovernorPerformance()
    {
#ifdef __ANDROID__
        if (!g_topology.is_snapdragon_8_gen_2)
    return;

  // Note: Nécessite root pour fonctionner
  // Alternative: utiliser PowerManager de Android
  INFO_LOG_FMT(COMMON, "CPU Governor optimization requested (requires root)");

  // Cette partie est informative - l'app ne peut pas changer le governor sans root
  // Mais on peut utiliser PowerManager pour suggérer au système
#endif
    }

    int GetRecommendedThreadCount()
    {
        if (g_topology.is_snapdragon_8_gen_2)
        {
            // Utiliser les Gold cores pour le threadpool
            return 4;  // Cores 1-4
        }

        // Fallback: utiliser tous les cores disponibles
        return g_topology.total_cores;
    }

}  // namespace AndroidCpuAffinity
