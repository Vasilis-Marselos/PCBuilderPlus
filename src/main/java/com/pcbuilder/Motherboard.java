package com.pcbuilder;

public class Motherboard {

    private final String id;
    private final String brand;
    private final String model;
    private final String socket;
    private final String formFactor;
    private final String memoryType;
    private final double priceEur;
    private final String manufacturerPartNumber;
    private final String skroutzQuery;
    private final String skroutzUrl;

    public Motherboard(String id, String brand, String model,
                       String socket, String formFactor, double priceEur) {
        this(id, brand, model, socket, formFactor, inferMemoryType(socket, model), priceEur,
                "", "", "");
    }

    public Motherboard(String id, String brand, String model,
                       String socket, String formFactor, double priceEur,
                       String manufacturerPartNumber,
                       String skroutzQuery,
                       String skroutzUrl) {
        this(id, brand, model, socket, formFactor, inferMemoryType(socket, model), priceEur,
                manufacturerPartNumber, skroutzQuery, skroutzUrl);
    }

    public Motherboard(String id, String brand, String model,
                       String socket, String formFactor, String memoryType, double priceEur,
                       String manufacturerPartNumber,
                       String skroutzQuery,
                       String skroutzUrl) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.socket = socket;
        this.formFactor = formFactor;
        this.memoryType = normalizeMemoryType(memoryType, socket, model);
        this.priceEur = priceEur;
        this.manufacturerPartNumber = manufacturerPartNumber != null ? manufacturerPartNumber : "";
        this.skroutzQuery = skroutzQuery != null ? skroutzQuery : "";
        this.skroutzUrl = skroutzUrl != null ? skroutzUrl : "";
    }

    public String id() { return id; }
    public String brand() { return brand; }
    public String model() { return model; }
    public String socket() { return socket; }
    public String formFactor() { return formFactor; }
    public String memoryType() { return memoryType; }
    public double priceEur() { return priceEur; }
    public String manufacturerPartNumber() { return manufacturerPartNumber; }
    public String skroutzQuery() { return skroutzQuery; }
    public String skroutzUrl() { return skroutzUrl; }

    private static String normalizeMemoryType(String memoryType, String socket, String model) {
        if (memoryType != null && !memoryType.isBlank()) {
            String v = memoryType.trim().toUpperCase();
            if (v.contains("DDR5")) return "DDR5";
            if (v.contains("DDR4")) return "DDR4";
            if (v.equals("D5")) return "DDR5";
            if (v.equals("D4")) return "DDR4";
        }
        return inferMemoryType(socket, model);
    }

    



    public static String inferMemoryType(String socket, String model) {
        String normalizedSocket = socket != null ? socket.trim().toLowerCase() : "";
        String normalizedModel = model != null ? model.trim().toLowerCase() : "";

        if (normalizedSocket.equals("am5")) return "DDR5";
        if (normalizedSocket.equals("am4")) return "DDR4";


        if (normalizedSocket.equals("lga1700")) {
            if (normalizedModel.contains("ddr4") || normalizedModel.contains(" d4") || normalizedModel.endsWith("d4")) {
                return "DDR4";
            }
            if (normalizedModel.contains("ddr5") || normalizedModel.contains(" d5") || normalizedModel.endsWith("d5")) {
                return "DDR5";
            }
        }

        return "UNKNOWN";
    }

    @Override
    public String toString() {
        String mem = memoryType != null && !memoryType.isBlank() && !memoryType.equals("UNKNOWN")
                ? ", " + memoryType
                : "";
        return brand + " " + model + " (" + socket + ", " + formFactor + mem + ", €" + priceEur + ")";
    }
}
