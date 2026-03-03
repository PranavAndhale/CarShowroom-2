package models;

import java.util.ArrayList;
import java.util.List;

public class Car {
    private int id;
    private String brand;
    private String model;
    private int year;
    private String color;
    private double price;
    private String category;
    private int stock;
    private List<String> features;
    private String imagePath;
    private boolean isAvailable;

    private String fuelType;
    private String transmission;
    private double engineSize;
    private String sketchfabModelId; // Sketchfab 3D model UID for the web configurator

    public Car() {
        this.features = new ArrayList<>();
        this.isAvailable = true;
    }

    public Car(int id, String brand, String model, int year, String color,
            double price, String category, int stock) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.color = color;
        this.price = price;
        this.category = category;
        this.stock = stock;
        this.features = new ArrayList<>();
        this.isAvailable = true;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String c) {
        this.category = c;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> f) {
        this.features = f;
    }

    public void addFeature(String feat) {
        this.features.add(feat);
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean avail) {
        this.isAvailable = avail;
    }

    // ── NEW getters / setters ──────────────────────────────────────────────────
    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String t) {
        this.transmission = t;
    }

    public double getEngineSize() {
        return engineSize;
    }

    public void setEngineSize(double engineSize) {
        this.engineSize = engineSize;
    }

    public String getSketchfabModelId() {
        return sketchfabModelId;
    }

    public void setSketchfabModelId(String sketchfabModelId) {
        this.sketchfabModelId = sketchfabModelId;
    }

    @Override
    public String toString() {
        return brand + " " + model + " (" + year + ") - $" + price;
    }

    public String getFullDetails() {
        return "ID: " + id + "\n" +
                "Brand: " + brand + "\n" +
                "Model: " + model + "\n" +
                "Year: " + year + "\n" +
                "Color: " + color + "\n" +
                "Price: $" + price + "\n" +
                "Category: " + category + "\n" +
                "Fuel: " + fuelType + "\n" +
                "Trans: " + transmission + "\n" +
                "Engine: " + engineSize + "L\n" +
                "Stock: " + stock + "\n" +
                "Available: " + (isAvailable ? "Yes" : "No");
    }
}
