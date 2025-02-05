package com.example.scraperremus;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ShopifyBatchSaver {

    // Stałe wartości, które będą takie same dla wszystkich produktów
    private static final String VENDOR = "Remus";
    private static final String PRODUCT_CATEGORY = "Pojazdy i części > Akcesoria i części do pojazdów > Części do pojazdów silnikowych > Elementy układu wydechowego";
    private static final String TYPE = "exhaust";
    private static final String TAGS = "Remus";
    private static final String PUBLISHED = "TRUE";
    private static final String VARIANT_INVENTORY_POLICY = "deny";
    private static final String VARIANT_FULFILLMENT_SERVICE = "manual";
    private static final String VARIANT_REQUIRES_SHIPPING = "TRUE";
    private static final String VARIANT_TAXABLE = "FALSE";
    private static final String GIFT_CARD = "FALSE";
    private static final String STATUS = "active";

    // Formatowanie ceny – np. "3120.00"
    private static final DecimalFormat priceFormat = new DecimalFormat("0.00");

    /**
     * Generuje jeden plik CSV dla listy produktów, generując wszystkie kombinacje opcji dla każdego produktu.
     */
    public static void saveForShopify(List<Product> products, String outputFilePath) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputFilePath))) {
            // Nagłówek CSV
            pw.println("Handle,Title,Body (HTML),Vendor,Product Category,Type,Tags,Published,Option1 Name,Option1 Value,Option1 Linked To,Option2 Name,Option2 Value,Option2 Linked To,Option3 Name,Option3 Value,Option3 Linked To,Variant SKU,Variant Grams,Variant Inventory Qty,Variant Inventory Policy,Variant Fulfillment Service,Variant Price,Variant Requires Shipping,Variant Taxable,Image Src,Image Position,Gift Card,Status");

            for (Product product : products) {
                String handle = slugify(product.getTitle());
                String body = product.getDescription();
                // Używamy wszystkich zdjęć – imageCount
                int imageCount = product.getImagesLinks() != null ? product.getImagesLinks().size() : 0;

                // Cena bazowa: przyjmujemy, że product.getPrice() zawiera finalPrice (np. "1500.00")
                double basePrice = 0.0;
                try {
                    String priceStr = product.getPrice().replace("€", "").replace(",", "").trim();
                    basePrice = Double.parseDouble(priceStr);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Generujemy kombinacje opcji
                List<List<OptionValue>> combinations = new ArrayList<>();
                if (product.getOptions() != null && !product.getOptions().isEmpty()) {
                    combinations = generateCombinations(product.getOptions());
                } else {
                    combinations.add(new ArrayList<>());
                }

                int rows = Math.max(combinations.size(), imageCount);
                boolean firstRow = true;
                for (int i = 0; i < rows; i++) {
                    // Dla opcji: jeśli kombinacja jest dostępna, używamy jej, inaczej puste wartości
                    List<OptionValue> combination = i < combinations.size() ? combinations.get(i) : new ArrayList<>();
                    double optionsSum = 0.0;
                    List<OptionGroup> groups = product.getOptions();
                    String option1Name = "", option1Value = "";
                    String option2Name = "", option2Value = "";
                    String option3Name = "", option3Value = "";
                    if (groups != null) {
                        for (int j = 0; j < groups.size(); j++) {
                            String val = "";
                            double adj = 0.0;
                            if (j < combination.size()) {
                                OptionValue ov = combination.get(j);
                                val = ov.getValue();
                                adj = ov.getPriceAdjustment();
                            }
                            optionsSum += adj;
                            if (j == 0) {
                                option1Name = groups.get(j).getName();
                                option1Value = val;
                            } else if (j == 1) {
                                option2Name = groups.get(j).getName();
                                option2Value = val;
                            } else if (j == 2) {
                                option3Name = groups.get(j).getName();
                                option3Value = val;
                            }
                        }
                    }
                    double variantPrice = (basePrice + optionsSum) / 1.2 * 1.23 * 4.2;
                    String variantPriceStr = priceFormat.format(variantPrice);

                    // SKU: tylko w pierwszym wierszu używamy SKU z produktu
                    String variantSKU = firstRow ? product.getSku() : "";

                    // Obraz: jeśli i < imageCount, to pobieramy i-ty obraz, inaczej pusty
                    String imageSrcCell = (product.getImagesLinks() != null && i < product.getImagesLinks().size()) ? product.getImagesLinks().get(i) : "";

                    // Pozostałe stałe kolumny – w pierwszym wierszu, w kolejnych puste
                    String handleCell = handle;
                    String titleCell = firstRow ? product.getTitle() : "";
                    String bodyCell = firstRow ? body : "";
                    String vendorCell = firstRow ? VENDOR : "";
                    String categoryCell = firstRow ? PRODUCT_CATEGORY : "";
                    String typeCell = firstRow ? TYPE : "";
                    String tagsCell = firstRow ? TAGS : "";
                    String publishedCell = firstRow ? PUBLISHED : "";
                    String variantInventoryPolicy =  VARIANT_INVENTORY_POLICY;
                    String variantFulfilment =  VARIANT_FULFILLMENT_SERVICE;
                    String variantReqShip = VARIANT_REQUIRES_SHIPPING;
                    String giftCard = firstRow ? GIFT_CARD : "";
                    String status = firstRow ? STATUS : "";

                    // Opcje Linked To – puste
                    String option1Linked = "";
                    String option2Linked = "";
                    String option3Linked = "";

                    // Pozostałe stałe kolumny wariantu
                    String variantGrams = "0";
                    String variantInventoryQty = "0";

                    String row = escapeCsv(handleCell) + "," +
                            escapeCsv(titleCell) + "," +
                            escapeCsv(bodyCell) + "," +
                            escapeCsv(vendorCell) + "," +
                            escapeCsv(categoryCell) + "," +
                            escapeCsv(typeCell) + "," +
                            escapeCsv(tagsCell) + "," +
                            escapeCsv(publishedCell) + "," +
                            (!option1Value.equals("") ? escapeCsv(option1Name) : "" ) + "," +
                            escapeCsv(option1Value) + "," +
                            escapeCsv(option1Linked) + "," +
                            (!option1Value.equals("") ? escapeCsv(option2Name) : "" ) + "," +
                            escapeCsv(option2Value) + "," +
                            escapeCsv(option2Linked) + "," +
                            (!option1Value.equals("") ? escapeCsv(option3Name): "" )  + "," +
                            escapeCsv(option3Value) + "," +
                            escapeCsv(option3Linked) + "," +
                            escapeCsv(variantSKU) + "," +
                            escapeCsv(variantGrams) + "," +
                            escapeCsv(variantInventoryQty) + "," +
                            escapeCsv(variantInventoryPolicy) + "," +
                            escapeCsv(variantFulfilment) + "," +
                            escapeCsv(variantPriceStr) + "," +
                            escapeCsv(variantReqShip) + "," +
                            escapeCsv(!imageSrcCell.equals("") ? VARIANT_TAXABLE : "") + "," +
                            escapeCsv(imageSrcCell) + "," +
                            (!imageSrcCell.equals("") ? i + 1  + "," : ",") +
                            escapeCsv(giftCard) + "," +
                            escapeCsv(status);
                    pw.println(row);
                    firstRow = false;
                }

                System.out.println("Plik CSV zapisany do: " + outputFilePath);
            }} catch(IOException e){
                e.printStackTrace();
            }

    }

        private static List<List<OptionValue>> generateCombinations (List < OptionGroup > groups) {
            List<List<OptionValue>> result = new ArrayList<>();
            generateCombinationsRecursive(groups, 0, new ArrayList<>(), result);
            return result;
        }

        private static void generateCombinationsRecursive (List < OptionGroup > groups,int index, List<
        OptionValue > current, List < List < OptionValue >> result){
            if (index == groups.size()) {
                result.add(new ArrayList<>(current));
                return;
            }
            for (OptionValue ov : groups.get(index).getValues()) {
                current.add(ov);
                generateCombinationsRecursive(groups, index + 1, current, result);
                current.remove(current.size() - 1);
            }
        }

        private static String slugify (String text){
            if (text == null) return "";
            return text.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        }

        private static String escapeCsv (String text){
            if (text == null) return "";
            if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
                text = text.replace("\"", "\"\"");
                return "\"" + text + "\"";
            }
            return text;
        }
    }





