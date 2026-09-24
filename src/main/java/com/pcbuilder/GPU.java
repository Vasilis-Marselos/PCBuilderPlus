package com.pcbuilder;

public class GPU {

    private final String id;
    private final String brand;
    private final String model;
    private final int vramGb;
    private final double perfScore;
    private final int tdpW;
    private final int lengthMm;
    private final double priceEur;
    private final String manufacturerPartNumber;
    private final String skroutzQuery;
    private final String skroutzUrl;

    public GPU(String id, String brand, String model,
               int vramGb, double perfScore, int tdpW, double priceEur) {
        this(id, brand, model, vramGb, perfScore, tdpW,
                inferDefaultLengthMm(id, brand, model), priceEur,
                "", "", "");
    }

    public GPU(String id, String brand, String model,
               int vramGb, double perfScore, int tdpW, double priceEur,
               String manufacturerPartNumber,
               String skroutzQuery,
               String skroutzUrl) {
        this(id, brand, model, vramGb, perfScore, tdpW,
                inferDefaultLengthMm(id, brand, model), priceEur,
                manufacturerPartNumber, skroutzQuery, skroutzUrl);
    }

    public GPU(String id, String brand, String model,
               int vramGb, double perfScore, int tdpW, int lengthMm, double priceEur,
               String manufacturerPartNumber,
               String skroutzQuery,
               String skroutzUrl) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.vramGb = vramGb;
        this.perfScore = perfScore;
        this.tdpW = tdpW;
        this.lengthMm = lengthMm > 0 ? lengthMm : inferDefaultLengthMm(id, brand, model);
        this.priceEur = priceEur;
        this.manufacturerPartNumber = manufacturerPartNumber != null ? manufacturerPartNumber : "";
        this.skroutzQuery = skroutzQuery != null ? skroutzQuery : "";
        this.skroutzUrl = skroutzUrl != null ? skroutzUrl : "";
    }

    public String id() { return id; }
    public String brand() { return brand; }
    public String model() { return model; }
    public int vramGb() { return vramGb; }
    public double perfScore() { return perfScore; }
    public int tdpW() { return tdpW; }
    public int lengthMm() { return lengthMm; }
    public double priceEur() { return priceEur; }
    public String manufacturerPartNumber() { return manufacturerPartNumber; }
    public String skroutzQuery() { return skroutzQuery; }
    public String skroutzUrl() { return skroutzUrl; }

    private static int inferDefaultLengthMm(String id, String brand, String model) {
        String text = ((id != null ? id : "") + " " +
                (brand != null ? brand : "") + " " +
                (model != null ? model : "")).toLowerCase();



        if (text.contains("5090")) return 304;
        if (text.contains("4090")) return 340;
        if (text.contains("4080")) return 320;
        if (text.contains("4070")) return 300;
        if (text.contains("7900")) return 320;
        if (text.contains("7800")) return 300;
        if (text.contains("7700")) return 280;
        if (text.contains("7600")) return 240;
        return 300;
    }

    @Override
    public String toString() {
        return brand + " " + model + " (" + vramGb + "GB, " +
                perfScore + " score, " + tdpW + "W, " + lengthMm + "mm, €" + priceEur + ")";
    }
}
