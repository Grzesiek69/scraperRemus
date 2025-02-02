package com.example.scraperremus;

public class OptionValue {
    private String value;
    private double priceAdjustment;

    public OptionValue(String value, double priceAdjustment) {
        this.value = value;
        this.priceAdjustment = priceAdjustment;
    }

    public String getValue() {
        return value;
    }

    public double getPriceAdjustment() {
        return priceAdjustment;
    }
}
