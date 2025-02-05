package com.example.scraperremus;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductScrapper {

    /**
     * Pobiera wszystkie URL-e produktów ze strony kategorii.
     */
    public static List<String> getProductUrlsFromCategory(String pageUrl) throws IOException {
        List<String> productUrls = new ArrayList<>();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(pageUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.product-item-link")));
            } catch (TimeoutException e) {
                System.out.println("Timeout oczekiwania na produkty – sprawdź selektor.");
            }
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = ((Number) js.executeScript("return document.body.scrollHeight")).longValue();
            while (true) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(2000);
                long newHeight = ((Number) js.executeScript("return document.body.scrollHeight")).longValue();
                if (newHeight == lastHeight) break;
                lastHeight = newHeight;
            }
            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);
            Elements productLinkElements = doc.select("a.product-item-link");
            for (Element link : productLinkElements) {
                String prodUrl = link.absUrl("href");
                if (!productUrls.contains(prodUrl)) {
                    productUrls.add(prodUrl);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return productUrls;
    }

    /**
     * Pobiera dane produktu przy użyciu Selenium (wyrenderowany HTML) i parsuje je przy pomocy Jsoup.
     */
    public static Product getProduct(String url) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.fieldset")));
            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);

            Product product = new Product();
            product.setUrl(url);

            String pageSource2 = "";
            URL fullUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) fullUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "text/html");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(fullUrl.openStream());
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();
                pageSource2 = response.toString();
            }

            // Tytuł
            String title = doc.select("h1.page-title").text();
            if (title == null || title.isEmpty()) {
                title = doc.select("meta[property=og:title]").attr("content");
            }
            if (title == null || title.isEmpty()) {
                title = "No title available";
            }
            product.setTitle(title);

            // Ekstrakcja SKU z <small class="text-muted"> (np. "SKU: 081023 1510-3")
            Element skuElem = doc.select("h1.page-title small.text-muted").first();
            if (skuElem != null) {
                String skuText = skuElem.text(); // np. "SKU: 081023 1510-3"
                skuText = skuText.replace("SKU:", "").trim();
                product.setSku(skuText);
            } else {
                product.setSku("");
            }

            // Cena – przyjmujemy cenę z elementu <span class="price">
            Element priceElement = doc.select("span.price").first();
            String price = (priceElement != null) ? priceElement.text() : "No price available";

            // Wyłuskujemy finalPrice z pageSource – szukamy fragmentu JSON zawierającego "finalPrice": {"amount": ...}
            String finalPriceStr = "";
            try {
                Pattern pattern = Pattern.compile(
                        "\"finalPrice\"\\s*:\\s*\\{\\s*\"amount\"\\s*:\\s*([0-9]{2,7})\\.[0-9]+\\s*}\\s*}\\s*,\\s*\"priceType\"\\s*:\\s*\"1\""
                );
                Matcher matcher = pattern.matcher(pageSource2);

                if (matcher.find()) {
                    finalPriceStr = matcher.group(1); // Pobiera tylko liczbę całkowitą przed kropką
                    System.out.println("Znaleziono finalPrice: " + finalPriceStr);
                } else {
                    System.out.println("Nie znaleziono wartości finalPrice.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!finalPriceStr.isEmpty()) {
                product.setPrice("€" + finalPriceStr);
            } else {
                product.setPrice(price); // Jeśli nie udało się znaleźć ceny, użyj domyślnej
            }

            // Waluta
            String currency = price.contains("€") ? "EUR" : "No currency available";
            product.setCurrency(currency);

            // Model auta – z breadcrumbs
            Elements breadcrumbs = doc.select("div.breadcrumbs a");
            String carModel = (breadcrumbs.size() > 1) ? breadcrumbs.get(1).text() : "No car model";
            product.setCarModel(carModel);

            // Opis
            String description = doc.select("div.product.attribute.description").text();
            if (description == null || description.isEmpty()) {
                description = "No description available";
            }
            product.setDescription(description);

// Zamiast wariantów – budujemy listę opcji (OptionGroup) na podstawie formularza
            List<OptionGroup> groups = new ArrayList<>();
            Element addToCartForm = doc.getElementById("product_addtocart_form");
            if (addToCartForm != null) {
                Elements optionDivs = addToCartForm.select("div.field.option");
                for (Element optionDiv : optionDivs) {
                    String groupTitle = optionDiv.select("label h5").text();
                    if (groupTitle == null || groupTitle.isEmpty()) continue;
                    List<OptionValue> values = new ArrayList<>();
                    Elements choices = optionDiv.select("div.field.choice");
                    for (Element choice : choices) {
                        String selectionName = choice.select("span.options-label .product-name").text();
                        if (selectionName == null || selectionName.isEmpty()) {
                            selectionName = choice.text();
                        }
                        Element priceElem = choice.select("span.price-wrapper span.price").first();
                        String selectionPrice = (priceElem != null) ? priceElem.text() : "";
                        double priceAdj = 0.0;
                        try {
                            String p = selectionPrice.replace("€", "").replace(",", "").trim();
                            if (!p.isEmpty()) {
                                priceAdj = Double.parseDouble(p);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        values.add(new OptionValue(selectionName, priceAdj));
                    }
                    groups.add(new OptionGroup(groupTitle, values));
                }
            }
            product.setOptions(groups);

            // Zdjęcia – pobieramy pełnowymiarowe zdjęcia z atrybutu data-img
            List<String> images = new ArrayList<>();
            Elements fullImageLinks = doc.select("div.product-vehicle-images a[data-img]");
            for (Element a : fullImageLinks) {
                String fullImgUrl = a.attr("data-img");
                if (fullImgUrl != null && !fullImgUrl.isEmpty() && !images.contains(fullImgUrl)) {
                    images.add(fullImgUrl);
                }
            }

            Elements variantImages = doc.select("a.optionsimage[data-img]");
            for (Element a : variantImages) {
                String variantImgUrl = a.attr("data-img");

                // Pomijamy obrazek, jeśli jego wariant to "No Selection"
                Element label = a.parent().parent().selectFirst("span.options-label");
                if (label != null && label.text().contains("No Selection")) {
                    continue;
                }

                if (variantImgUrl != null && !variantImgUrl.isEmpty() && !images.contains(variantImgUrl)) {
                    images.add(variantImgUrl);
                }
            }

            product.setImagesLinks(images);

            System.out.println(product);
            return product;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return null;
    }
}
