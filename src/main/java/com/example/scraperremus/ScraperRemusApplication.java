package com.example.scraperremus;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class ScraperRemusApplication {

	public static void main(String[] args) {
		// Dla pewności wypisujemy katalog roboczy
		System.out.println("Working Directory: " + System.getProperty("user.dir"));

		Scanner scanner = new Scanner(System.in);
		System.out.println("Wklej link (lub wpisz 'exit', aby zakończyć):");

		List<Product> allProducts = new ArrayList<>();
		String input = "";
		while (!(input = scanner.nextLine()).equalsIgnoreCase("exit")) {
			if (isCategoryPage(input)) {
				try {
					List<String> productUrls = ProductScrapper.getProductUrlsFromCategory(input);
					System.out.println("Znaleziono " + productUrls.size() + " produktów w kategorii.");
					for (String prodUrl : productUrls) {
						System.out.println("Przetwarzanie produktu: " + prodUrl);
						Product product = ProductScrapper.getProduct(prodUrl);
						if (product != null) {
							allProducts.add(product);
						}
					}
				} catch (Exception e) {
					System.err.println("Błąd podczas przetwarzania kategorii: " + e.getMessage());
				}
			} else {
				Product product = ProductScrapper.getProduct(input);
				if (product != null) {
					allProducts.add(product);
				}
			}
// Po zakończeniu zbierania wszystkich produktów
			ShopifyBatchSaver.saveForShopify(allProducts, "shopify_import.csv");
			System.out.println("Dziękuję, koniec.");
		}}

	private static boolean isCategoryPage(String url) {
		// Jeżeli URL zawiera ?vehicleId= bez dodatkowego slug-a po /car/ traktujemy to jako stronę kategorii
		return url.matches("https://remus\\.eu/en/car\\?.*");
	}
}
