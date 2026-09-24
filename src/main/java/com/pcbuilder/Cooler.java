package com.pcbuilder;








public class Cooler {

    public enum CoolerType {
        AIR,
        AIO
    }

    private final String id;
    private final String brand;
    private final String model;
    private final CoolerType type;
    private final int heightMm;
    private final int radiatorSizeMm;
    private final int tdpRatingW;
    private final double priceEur;
    private final String manufacturerPartNumber;
    private final String skroutzQuery;
    private final String skroutzUrl;

    public Cooler(String id,
                  String brand,
                  String model,
                  CoolerType type,
                  int heightMm,
                  int radiatorSizeMm,
                  int tdpRatingW,
                  double priceEur,
                  String manufacturerPartNumber,
                  String skroutzQuery,
                  String skroutzUrl) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.type = type;
        this.heightMm = heightMm;
        this.radiatorSizeMm = radiatorSizeMm;
        this.tdpRatingW = tdpRatingW;
        this.priceEur = priceEur;
        this.manufacturerPartNumber = manufacturerPartNumber != null ? manufacturerPartNumber : "";
        this.skroutzQuery = skroutzQuery != null ? skroutzQuery : "";
        this.skroutzUrl = skroutzUrl != null ? skroutzUrl : "";
    }

    public String id() { return id; }
    public String brand() { return brand; }
    public String model() { return model; }
    public CoolerType type() { return type; }
    public int heightMm() { return heightMm; }
    public int radiatorSizeMm() { return radiatorSizeMm; }
    public int tdpRatingW() { return tdpRatingW; }
    public double priceEur() { return priceEur; }
    public String manufacturerPartNumber() { return manufacturerPartNumber; }
    public String skroutzQuery() { return skroutzQuery; }
    public String skroutzUrl() { return skroutzUrl; }

    public boolean isAirCooler() {
        return type == CoolerType.AIR;
    }

    public boolean isAioCooler() {
        return type == CoolerType.AIO;
    }

    @Override
    public String toString() {
        if (isAirCooler()) {
            return brand + " " + model +
                    " (Air, " + heightMm + "mm height, " +
                    tdpRatingW + "W rating, €" + priceEur + ")";
        }

        return brand + " " + model +
                " (AIO " + radiatorSizeMm + "mm, " +
                tdpRatingW + "W rating, €" + priceEur + ")";
    }
}
