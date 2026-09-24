package com.pcbuilder;

import com.pcbuilder.data.CsvCatalogRepository;
import com.pcbuilder.services.CompatibilityService;
import com.pcbuilder.services.PowerEstimatorService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Offline regression scenarios added when preparing the public repository. */
class CoreServicesTest {
    private final CompatibilityService compatibility = new CompatibilityService(100);
    private final CPU cpu = new CPU("cpu", "Demo", "CPU", "AM5", 6, 4.0, 70, 65, 200);
    private final Motherboard board = new Motherboard("board", "Demo", "AM5 board", "AM5", "ATX", 150);
    private final RAM ram = new RAM("ram", "Demo", "DDR5", 32, 2, 6000, 100);
    private final GPU gpu = new GPU("gpu", "Demo", "GPU", 8, 70, 200, 300, 300, "", "", "");
    private final PCCase pcCase = new PCCase("case", "Demo", "Case", "ATX;Micro-ATX;Mini-ITX",
            300, 160, true, false, false, 80, "", "", "");
    private final Cooler cooler = new Cooler("cooler", "Demo", "Air", Cooler.CoolerType.AIR,
            160, 0, 150, 40, "", "", "");

    private String check(CPU c, Motherboard b, RAM r, GPU g, PSU p, int power) {
        return compatibility.buildCompatibilityMessage(c, b, pcCase, cooler, r, g, p, power);
    }

    @Test void validBuildAcceptsExactPhysicalClearanceAndSafePowerMargin() {
        String result = check(cpu, board, ram, gpu, new PSU("p", "Demo", "PSU", 650, 80), 350);
        assertFalse(result.contains("ΠΡΟΕΙΔΟΠΟΙΗΣΗ"), result);
        assertTrue(result.contains("PSU OK"), result);
        assertTrue(result.contains("Sockets OK"), result);
    }

    @Test void incompatibleSocketIsFlagged() {
        CPU am4 = new CPU("c", "Demo", "AM4", "AM4", 6, 4, 60, 65, 100);
        assertTrue(check(am4, board, ram, gpu, null, 350).contains("δεν ταιριάζει με το socket"));
    }

    @Test void incompatibleMemoryIsFlagged() {
        RAM ddr4 = new RAM("r", "Demo", "DDR4", 16, 2, 3200, 50);
        assertTrue(check(cpu, board, ddr4, gpu, null, 350).contains("δεν ταιριάζει με την υποστήριξη μνήμης"));
    }

    @Test void oversizedGraphicsCardIsFlagged() {
        GPU longGpu = new GPU("g", "Demo", "Long", 8, 70, 200, 301, 300, "", "", "");
        assertTrue(check(cpu, board, ram, longGpu, null, 350).contains("GPU είναι πολύ μακριά"));
    }

    @Test void powerSupplyCapacityBoundariesAreDistinguished() {
        assertTrue(check(cpu, board, ram, gpu, new PSU("p", "D", "P", 450, 50), 350).contains("PSU OK"));
        assertTrue(check(cpu, board, ram, gpu, new PSU("p", "D", "P", 449, 50), 350).contains("PSU οριακό"));
        assertTrue(check(cpu, board, ram, gpu, new PSU("p", "D", "P", 349, 50), 350).contains("PSU ΑΝΕΠΑΡΚΕΣ"));
    }

    @Test void emptyBuildDoesNotReportACompatibleCompleteSystem() {
        String result = compatibility.buildCompatibilityMessage(null, null, null, null, null, null, null, 0);
        assertTrue(result.contains("επίλεξε CPU + μητρική"));
        assertFalse(result.contains("PSU OK"));
        assertEquals(0, new PowerEstimatorService().estimatePower(null, null, null, null, null));
    }

    @Test void estimatedPowerIncludesComponentsAndSystemAllowance() {
        Storage storage = new Storage("s", "D", "NVMe", "NVMe", 1000, 8, 70);
        assertEquals(355, new PowerEstimatorService().estimatePower(cpu, gpu, board, ram, storage));
    }

    @Test void allBundledCatalogsLoadWithoutNetwork() throws Exception {
        CsvCatalogRepository repo = new CsvCatalogRepository(CoreServicesTest.class);
        assertAll(
            () -> assertFalse(repo.loadCpuCatalog().isEmpty()),
            () -> assertFalse(repo.loadGpuCatalog().isEmpty()),
            () -> assertFalse(repo.loadMotherboardCatalog().isEmpty()),
            () -> assertFalse(repo.loadRamCatalog().isEmpty()),
            () -> assertFalse(repo.loadStorageCatalog().isEmpty()),
            () -> assertFalse(repo.loadPsuCatalog().isEmpty()),
            () -> assertFalse(repo.loadCaseCatalog().isEmpty()),
            () -> assertFalse(repo.loadCoolerCatalog().isEmpty())
        );
    }
}
