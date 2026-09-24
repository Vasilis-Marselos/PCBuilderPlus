package com.pcbuilder;

public class PSU {

    private final String id;
    private final String brand;
    private final String model;
    private final int wattageW;
    private final double priceEur;
    private final String manufacturerPartNumber;
    private final String skroutzQuery;
    private final String skroutzUrl;

    public PSU(String id, String brand, String model,
               int wattageW, double priceEur) {
        this(id, brand, model, wattageW, priceEur,
                "", "", "");
    }

    public PSU(String id, String brand, String model,
               int wattageW, double priceEur,
               String manufacturerPartNumber,
               String skroutzQuery,
               String skroutzUrl) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.wattageW = wattageW;
        this.priceEur = priceEur;
        this.manufacturerPartNumber = manufacturerPartNumber != null ? manufacturerPartNumber : "";
        this.skroutzQuery = skroutzQuery != null ? skroutzQuery : "";
        this.skroutzUrl = skroutzUrl != null ? skroutzUrl : "";
    }

    public String id() { return id; }
    public String brand() { return brand; }
    public String model() { return model; }
    public int wattageW() { return wattageW; }
    public double priceEur() { return priceEur; }
    public String manufacturerPartNumber() { return manufacturerPartNumber; }
    public String skroutzQuery() { return skroutzQuery; }
    public String skroutzUrl() { return skroutzUrl; }

    @Override
    public String toString() {
        return brand + " " + model + " (" + wattageW + "W, €" + priceEur + ")";
    }
}
