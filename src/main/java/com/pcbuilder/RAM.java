package com.pcbuilder;

public class RAM {

    private final String id;
    private final String brand;
    private final String model;
    private final int capacityGb;
    private final int sticks;
    private final int speedMhz;
    private final double priceEur;
    private final String manufacturerPartNumber;
    private final String skroutzQuery;
    private final String skroutzUrl;

    public RAM(String id, String brand, String model,
               int capacityGb, int sticks, int speedMhz,
               double priceEur) {
        this(id, brand, model, capacityGb, sticks, speedMhz, priceEur,
                "", "", "");
    }

    public RAM(String id, String brand, String model,
               int capacityGb, int sticks, int speedMhz,
               double priceEur,
               String manufacturerPartNumber,
               String skroutzQuery,
               String skroutzUrl) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.capacityGb = capacityGb;
        this.sticks = sticks;
        this.speedMhz = speedMhz;
        this.priceEur = priceEur;
        this.manufacturerPartNumber = manufacturerPartNumber != null ? manufacturerPartNumber : "";
        this.skroutzQuery = skroutzQuery != null ? skroutzQuery : "";
        this.skroutzUrl = skroutzUrl != null ? skroutzUrl : "";
    }

    public String id() { return id; }
    public String brand() { return brand; }
    public String model() { return model; }
    public int capacityGb() { return capacityGb; }
    public int sticks() { return sticks; }
    public int speedMhz() { return speedMhz; }
    public double priceEur() { return priceEur; }
    public String manufacturerPartNumber() { return manufacturerPartNumber; }
    public String skroutzQuery() { return skroutzQuery; }
    public String skroutzUrl() { return skroutzUrl; }

    @Override
    public String toString() {
        return brand + " " + model +
                " (" + capacityGb + "GB, " +
                sticks + "x, " + speedMhz + " MHz, €" + priceEur + ")";
    }
}
