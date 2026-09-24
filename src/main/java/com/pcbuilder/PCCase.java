package com.pcbuilder;










public class PCCase {

    private final String id;
    private final String brand;
    private final String model;
    private final String supportedFormFactors;
    private final int maxGpuLengthMm;
    private final int maxAirCoolerHeightMm;
    private final boolean supports240Aio;
    private final boolean supports280Aio;
    private final boolean supports360Aio;
    private final double priceEur;
    private final String manufacturerPartNumber;
    private final String skroutzQuery;
    private final String skroutzUrl;

    public PCCase(String id, String brand, String model,
                  String supportedFormFactors,
                  int maxGpuLengthMm,
                  int maxAirCoolerHeightMm,
                  boolean supports240Aio,
                  boolean supports280Aio,
                  boolean supports360Aio,
                  double priceEur,
                  String manufacturerPartNumber,
                  String skroutzQuery,
                  String skroutzUrl) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.supportedFormFactors = supportedFormFactors;
        this.maxGpuLengthMm = maxGpuLengthMm;
        this.maxAirCoolerHeightMm = maxAirCoolerHeightMm;
        this.supports240Aio = supports240Aio;
        this.supports280Aio = supports280Aio;
        this.supports360Aio = supports360Aio;
        this.priceEur = priceEur;
        this.manufacturerPartNumber = manufacturerPartNumber != null ? manufacturerPartNumber : "";
        this.skroutzQuery = skroutzQuery != null ? skroutzQuery : "";
        this.skroutzUrl = skroutzUrl != null ? skroutzUrl : "";
    }

    public String id() { return id; }
    public String brand() { return brand; }
    public String model() { return model; }
    public String supportedFormFactors() { return supportedFormFactors; }
    public int maxGpuLengthMm() { return maxGpuLengthMm; }
    public int maxAirCoolerHeightMm() { return maxAirCoolerHeightMm; }
    public boolean supports240Aio() { return supports240Aio; }
    public boolean supports280Aio() { return supports280Aio; }
    public boolean supports360Aio() { return supports360Aio; }
    public double priceEur() { return priceEur; }
    public String manufacturerPartNumber() { return manufacturerPartNumber; }
    public String skroutzQuery() { return skroutzQuery; }
    public String skroutzUrl() { return skroutzUrl; }

    public boolean supportsMotherboardFormFactor(String formFactor) {
        if (formFactor == null || formFactor.isBlank()) {
            return false;
        }

        String target = normalizeFormFactor(formFactor);

        String[] supported = supportedFormFactors.split(";");
        for (String item : supported) {
            String current = normalizeFormFactor(item);
            if (current.equals(target)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeFormFactor(String value) {
        if (value == null) {
            return "";
        }

        String v = value.trim()
                .toLowerCase()
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");


        if (v.equals("microatx") || v.equals("matx") || v.equals("uatx")) {
            return "matx";
        }

        if (v.equals("miniitx") || v.equals("itx")) {
            return "itx";
        }

        if (v.equals("eatx")) {
            return "eatx";
        }

        if (v.equals("atx")) {
            return "atx";
        }

        return v;
    }

    @Override
    public String toString() {
        return brand + " " + model +
                " (supports " + supportedFormFactors +
                ", max GPU " + maxGpuLengthMm + "mm" +
                ", max cooler " + maxAirCoolerHeightMm + "mm" +
                ", €" + priceEur + ")";
    }
}