package com.pcbuilder.pricing;

public enum PriceConfidence {
    EXACT(4, true, "exact"),
    HIGH(3, true, "high"),
    MEDIUM(2, false, "medium"),
    LOW(1, false, "low"),
    FALLBACK(0, false, "csv");

    private final int rank;
    private final boolean usableForTotals;
    private final String label;

    PriceConfidence(int rank, boolean usableForTotals, String label) {
        this.rank = rank;
        this.usableForTotals = usableForTotals;
        this.label = label;
    }

    public int rank() {
        return rank;
    }

    public boolean usableForTotals() {
        return usableForTotals;
    }

    public String label() {
        return label;
    }

    public PriceConfidence downgrade() {
        return switch (this) {
            case EXACT -> HIGH;
            case HIGH -> MEDIUM;
            case MEDIUM -> LOW;
            case LOW, FALLBACK -> LOW;
        };
    }
}
