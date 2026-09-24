package com.pcbuilder.services;

import com.pcbuilder.CPU;
import com.pcbuilder.GPU;
import com.pcbuilder.RAM;
import com.pcbuilder.Storage;




public class BuildScoringService {

    public double computeCpuGpuPerf(CPU cpu, GPU gpu, boolean gamingProfile) {
        double perf = 0.0;
        double weightCpu;
        double weightGpu;

        if (gamingProfile) {
            weightCpu = 0.4;
            weightGpu = 0.6;
        } else {
            weightCpu = 0.6;
            weightGpu = 0.4;
        }

        if (cpu != null) perf += weightCpu * cpu.perfScore();
        if (gpu != null) perf += weightGpu * gpu.perfScore();

        return perf;
    }

    public double computeRamScore(RAM ram) {
        if (ram == null) return 0.0;

        double score = 0.0;
        if (ram.capacityGb() >= 16) score += 3.0;
        if (ram.capacityGb() >= 32) score += 3.0;
        if (ram.capacityGb() >= 64) score += 1.0;

        if (ram.speedMhz() >= 5200) score += 1.0;
        if (ram.speedMhz() >= 5600) score += 1.0;
        if (ram.speedMhz() >= 6000) score += 1.0;

        if (score > 10.0) score = 10.0;
        return score;
    }

    public double computeStorageScore(Storage storage) {
        if (storage == null) return 0.0;

        return Math.max(0.0, Math.min(10.0, storage.perfScore()));
    }

    public double computeOverallPerf(CPU cpu, GPU gpu, RAM ram, Storage storage, boolean gamingProfile) {
        double base = computeCpuGpuPerf(cpu, gpu, gamingProfile);
        double ramScore = computeRamScore(ram);
        double storageScore = computeStorageScore(storage);

        double ramWeight = gamingProfile ? 0.2 : 0.4;
        double storageWeight = gamingProfile ? 0.1 : 0.2;

        return base + ramWeight * ramScore + storageWeight * storageScore;
    }
}
