package com.pcbuilder.services;

import com.pcbuilder.CPU;
import com.pcbuilder.GPU;
import com.pcbuilder.Motherboard;
import com.pcbuilder.RAM;
import com.pcbuilder.Storage;







public class PowerEstimatorService {

    public int estimatePower(CPU cpu, GPU gpu, Motherboard mobo, RAM ram, Storage storage) {
        int estimated = 0;
        if (cpu != null) estimated += cpu.tdpW();
        if (gpu != null) estimated += gpu.tdpW();
        if (ram != null) estimated += 10;
        if (storage != null) estimated += 5;


        if (cpu != null || gpu != null || mobo != null || ram != null || storage != null) {
            estimated += 75;
        }

        return estimated;
    }
}
