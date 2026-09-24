package com.pcbuilder.pricing;

public enum MarketplaceCategory {
    CPU("CPU", "επεξεργαστής"),
    GPU("GPU", "κάρτα γραφικών"),
    MOTHERBOARD("Motherboard", "μητρική"),
    RAM("RAM", "RAM DDR5"),
    STORAGE("Storage", "SSD NVMe αποθηκευτικός χώρος"),
    PSU("PSU", "τροφοδοτικό"),
    CASE("Case", "κουτί υπολογιστή"),
    COOLER("Cooler", "ψύκτρα επεξεργαστή υδρόψυξη AIO");

    private final String displayName;
    private final String searchSuffix;

    MarketplaceCategory(String displayName, String searchSuffix) {
        this.displayName = displayName;
        this.searchSuffix = searchSuffix;
    }

    public String displayName() {
        return displayName;
    }

    public String searchSuffix() {
        return searchSuffix;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
