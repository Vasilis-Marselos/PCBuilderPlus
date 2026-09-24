package com.pcbuilder;

public class Storage {

    private final String id;
    private final String brand;
    private final String model;
    private final String type;
    private final int capacityGb;
    private final double perfScore;
    private final double priceEur;
    private final String manufacturerPartNumber;
    private final String skroutzQuery;
    private final String skroutzUrl;

    public Storage(String id, String brand, String model,
                   String type, int capacityGb,
                   double perfScore, double priceEur) {
        this(id, brand, model, type, capacityGb, perfScore, priceEur,
                "", "", "");
    }

    public Storage(String id, String brand, String model,
                   String type, int capacityGb,
                   double perfScore, double priceEur,
                   String manufacturerPartNumber,
                   String skroutzQuery,
                   String skroutzUrl) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.type = type;
        this.capacityGb = capacityGb;
        this.perfScore = perfScore;
        this.priceEur = priceEur;
        this.manufacturerPartNumber = manufacturerPartNumber != null ? manufacturerPartNumber : "";
        this.skroutzQuery = skroutzQuery != null ? skroutzQuery : "";
        this.skroutzUrl = skroutzUrl != null ? skroutzUrl : "";
    }

    public String id() { return id; }
    public String brand() { return brand; }
    public String model() { return model; }
    public String type() { return type; }
    public int capacityGb() { return capacityGb; }
    public double perfScore() { return perfScore; }
    public double priceEur() { return priceEur; }
    public String manufacturerPartNumber() { return manufacturerPartNumber; }
    public String skroutzQuery() { return skroutzQuery; }
    public String skroutzUrl() { return skroutzUrl; }

    @Override
    public String toString() {
        return brand + " " + model +
                " (" + type + ", " + capacityGb + "GB, score " + perfScore +
                ", €" + priceEur + ")";
    }
}
