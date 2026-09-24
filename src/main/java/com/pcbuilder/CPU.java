package com.pcbuilder;

public class CPU {

    private final String id;
    private final String brand;
    private final String model;
    private final String socket;
    private final int cores;
    private final double baseClockGhz;
    private final double perfScore;
    private final int tdpW;
    private final double priceEur;
    private final String manufacturerPartNumber;
    private final String skroutzQuery;
    private final String skroutzUrl;

    public CPU(String id, String brand, String model,
               String socket,
               int cores, double baseClockGhz,
               double perfScore, int tdpW, double priceEur) {
        this(id, brand, model, socket, cores, baseClockGhz, perfScore, tdpW, priceEur,
                "", "", "");
    }

    public CPU(String id, String brand, String model,
               String socket,
               int cores, double baseClockGhz,
               double perfScore, int tdpW, double priceEur,
               String manufacturerPartNumber,
               String skroutzQuery,
               String skroutzUrl) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.socket = socket;
        this.cores = cores;
        this.baseClockGhz = baseClockGhz;
        this.perfScore = perfScore;
        this.tdpW = tdpW;
        this.priceEur = priceEur;
        this.manufacturerPartNumber = manufacturerPartNumber != null ? manufacturerPartNumber : "";
        this.skroutzQuery = skroutzQuery != null ? skroutzQuery : "";
        this.skroutzUrl = skroutzUrl != null ? skroutzUrl : "";
    }

    public String id() { return id; }
    public String brand() { return brand; }
    public String model() { return model; }
    public String socket() { return socket; }
    public int cores() { return cores; }
    public double baseClockGhz() { return baseClockGhz; }
    public double perfScore() { return perfScore; }
    public int tdpW() { return tdpW; }
    public double priceEur() { return priceEur; }
    public String manufacturerPartNumber() { return manufacturerPartNumber; }
    public String skroutzQuery() { return skroutzQuery; }
    public String skroutzUrl() { return skroutzUrl; }

    @Override
    public String toString() {
        return brand + " " + model + " (" + socket + ", " +
                cores + "C, " + baseClockGhz + " GHz, " +
                tdpW + "W TDP, score " + perfScore + ", €" + priceEur + ")";
    }
}
