package com.pcbuilder.services;

import com.pcbuilder.CPU;
import com.pcbuilder.Cooler;
import com.pcbuilder.GPU;
import com.pcbuilder.Motherboard;
import com.pcbuilder.PCCase;
import com.pcbuilder.PSU;
import com.pcbuilder.RAM;




public class CompatibilityService {

    private final int minSafeHeadroomW;

    public CompatibilityService(int minSafeHeadroomW) {
        this.minSafeHeadroomW = minSafeHeadroomW;
    }

    public String buildCompatibilityMessage(CPU cpu,
                                            Motherboard mobo,
                                            PCCase pcCase,
                                            Cooler cooler,
                                            RAM ram,
                                            GPU gpu,
                                            PSU psu,
                                            int estimatedPower) {
        StringBuilder msg = new StringBuilder();


        if (cpu != null && mobo != null) {
            String cpuSocket = cpu.socket();
            String moboSocket = mobo.socket();

            if (cpuSocket.equalsIgnoreCase(moboSocket)) {
                msg.append("Sockets OK (").append(cpuSocket).append("). ");
            } else {
                msg.append("ΠΡΟΕΙΔΟΠΟΙΗΣΗ: το socket CPU ").append(cpuSocket)
                        .append(" δεν ταιριάζει με το socket μητρικής ")
                        .append(moboSocket).append(". ");
            }
        } else {
            msg.append("Sockets: επίλεξε CPU + μητρική. ");
        }


        if (ram != null && mobo != null) {
            String ramType = inferRamType(ram);
            String motherboardMemoryType = inferMotherboardMemoryType(mobo);

            if (!ramType.equals("UNKNOWN") && !motherboardMemoryType.equals("UNKNOWN")) {
                if (ramType.equals(motherboardMemoryType)) {
                    msg.append("Τύπος RAM OK (").append(ramType).append("). ");
                } else {
                    msg.append("ΠΡΟΕΙΔΟΠΟΙΗΣΗ: τύπος RAM ")
                            .append(ramType)
                            .append(" δεν ταιριάζει με την υποστήριξη μνήμης της μητρικής ")
                            .append(motherboardMemoryType)
                            .append(". ");
                }
            } else {
                msg.append("Τύπος RAM: δεν ήταν δυνατή η πλήρης επιβεβαίωση RAM/μητρικής. ");
            }
        } else {
            msg.append("Τύπος RAM: επίλεξε RAM + μητρική. ");
        }



        if (mobo != null && pcCase != null) {
            if (pcCase.supportsMotherboardFormFactor(mobo.formFactor())) {
                msg.append("Το κουτί υποστηρίζει ").append(mobo.formFactor()).append(" μητρική. ");
            } else {
                msg.append("ΠΡΟΕΙΔΟΠΟΙΗΣΗ: το κουτί δεν υποστηρίζει ")
                        .append(mobo.formFactor())
                        .append(" μητρική. ");
            }
        } else {
            msg.append("Κουτί: επίλεξε μητρική + κουτί. ");
        }


        if (gpu != null && pcCase != null) {
            int gpuLength = gpu.lengthMm();
            if (gpuLength <= pcCase.maxGpuLengthMm()) {
                msg.append("Μήκος GPU OK (")
                        .append(gpuLength)
                        .append("mm <= ")
                        .append(pcCase.maxGpuLengthMm())
                        .append("mm). ");
            } else {
                msg.append("ΠΡΟΕΙΔΟΠΟΙΗΣΗ: η GPU είναι πολύ μακριά (")
                        .append(gpuLength)
                        .append("mm > ")
                        .append(pcCase.maxGpuLengthMm())
                        .append("mm). ");
            }
        } else {
            msg.append("Χώρος GPU: επίλεξε GPU + κουτί. ");
        }


        if (cooler != null && pcCase != null) {
            if (cooler.isAirCooler()) {
                if (cooler.heightMm() <= pcCase.maxAirCoolerHeightMm()) {
                    msg.append("Ύψος αερόψυξης OK (")
                            .append(cooler.heightMm())
                            .append("mm <= ")
                            .append(pcCase.maxAirCoolerHeightMm())
                            .append("mm). ");
                } else {
                    msg.append("ΠΡΟΕΙΔΟΠΟΙΗΣΗ: η αερόψυξη μπορεί να είναι πολύ ψηλή (")
                            .append(cooler.heightMm())
                            .append("mm > ")
                            .append(pcCase.maxAirCoolerHeightMm())
                            .append("mm). ");
                }
            } else if (cooler.isAioCooler()) {
                boolean supported = switch (cooler.radiatorSizeMm()) {
                    case 240 -> pcCase.supports240Aio();
                    case 280 -> pcCase.supports280Aio();
                    case 360 -> pcCase.supports360Aio();
                    default -> false;
                };

                if (supported) {
                    msg.append("AIO radiator OK (")
                            .append(cooler.radiatorSizeMm())
                            .append("mm υποστηρίζεται). ");
                } else {
                    msg.append("ΠΡΟΕΙΔΟΠΟΙΗΣΗ: το κουτί μπορεί να μην υποστηρίζει ")
                            .append(cooler.radiatorSizeMm())
                            .append("mm AIO radiator. ");
                }
            }
        } else {
            msg.append("Ψύξη CPU: επίλεξε ψύξη + κουτί. ");
        }


        if (estimatedPower > 0 && psu != null) {
            int headroom = psu.wattageW() - estimatedPower;
            if (headroom >= minSafeHeadroomW) {
                msg.append("PSU OK (")
                        .append(psu.wattageW()).append("W, ~")
                        .append(headroom).append("W περιθώριο).");
            } else if (headroom >= 0) {
                msg.append("PSU οριακό: μόνο ~")
                        .append(headroom).append("W περιθώριο (")
                        .append(psu.wattageW()).append("W PSU vs ~")
                        .append(estimatedPower).append("W κατανάλωση).");
            } else {
                msg.append("PSU ΑΝΕΠΑΡΚΕΣ: PSU ")
                        .append(psu.wattageW()).append("W < εκτίμηση ")
                        .append(estimatedPower).append("W. Επίλεξε ισχυρότερο PSU.");
            }
        } else if (estimatedPower > 0) {
            msg.append(" Επίλεξε PSU για έλεγχο ισχύος.");
        } else {
            msg.append(" Ισχύς: επίλεξε τουλάχιστον CPU ή GPU για εκτίμηση κατανάλωσης.");
        }

        return msg.toString();
    }

    private String inferRamType(RAM ram) {
        if (ram == null) return "UNKNOWN";

        String text = (ram.brand() + " " + ram.model()).toLowerCase();
        int speed = ram.speedMhz();

        if (text.contains("ddr5") || speed >= 4800) {
            return "DDR5";
        }

        if (text.contains("ddr4") || speed <= 4000) {
            return "DDR4";
        }

        return "UNKNOWN";
    }

    private String inferMotherboardMemoryType(Motherboard mobo) {
        if (mobo == null) return "UNKNOWN";

        String explicitType = mobo.memoryType();
        if (explicitType != null && !explicitType.isBlank() && !explicitType.equals("UNKNOWN")) {
            return explicitType;
        }


        return Motherboard.inferMemoryType(mobo.socket(), mobo.model());
    }

}
