package com.example.scraperremus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class Product {
    private String url;
    private String title;
    // Cena bazowa – przyjmujemy wartość finalPrice (bez symbolu waluty)
    private String price;
    private String currency;
    private String carModel;
    private String sku;

    private String description;
    private List<Variant> variants;
    private List<String> imagesLinks;
    // Nowe pole: lista grup opcji dla Shopify
    private List<OptionGroup> options;

    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Getter
    @Setter
    public static class Variant {
        private String optionName;
        // Cena wariantu (np. "€1,620.00")
        private String price;
    }
}
