package com.example.scraperremus;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ProductSaver {

    private static final String FILE_NAME = "remus.xlsx";

    public static void saveProductToExcel(Product product) {
        Workbook workbook;
        Sheet sheet;

        try {
            File file = new File(FILE_NAME);

            // Sprawdzamy, czy plik istnieje i zawiera dane
            if (file.exists() && file.length() > 0) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = WorkbookFactory.create(fis);
                }
            } else {
                workbook = new XSSFWorkbook(); // Nowy plik, gdy nie istnieje lub jest pusty
            }

            // Pobieramy lub tworzymy arkusz "Products"
            sheet = workbook.getSheet("Products");
            if (sheet == null) {
                sheet = workbook.createSheet("Products");
                createHeaderRow(sheet);
            }

            // Ustalamy indeks nowego wiersza
            int rowIndex = sheet.getLastRowNum() + 1;

            List<String> images = product.getImagesLinks();
            List<Product.Variant> variants = product.getVariants();

            int variantCount = variants.size();
            int imageCount = images.size();
            int maxRows = Math.max(variantCount, imageCount);

            for (int i = 0; i < maxRows; i++) {
                Row row = sheet.createRow(rowIndex++);
                if (i < imageCount) {
                    row.createCell(6).setCellValue(images.get(i)); // Link do zdjęcia
                }

                if (i < variantCount) {
                    Product.Variant variant = variants.get(i);
                    if (i == 0) {
                        // Pierwszy wiersz – pełne dane produktu
                        row.createCell(0).setCellValue(product.getUrl());
                        row.createCell(1).setCellValue(product.getTitle());
                        row.createCell(2).setCellValue(variant.getPrice());
                        row.createCell(3).setCellValue(product.getCurrency());
                        row.createCell(4).setCellValue(product.getCarModel());
                        row.createCell(5).setCellValue(product.getDescription());
                        row.createCell(7).setCellValue(variant.getOptionName());
                    } else {
                        // Kolejne warianty – tylko dane wariantu oraz waluta
                        row.createCell(2).setCellValue(variant.getPrice());
                        row.createCell(3).setCellValue(product.getCurrency());
                        row.createCell(7).setCellValue(variant.getOptionName());
                    }
                } else if (i >= variantCount && i == 0 && variantCount == 0) {
                    // Brak wariantów – zapis "default" tylko przy pierwszym zdjęciu
                    row.createCell(0).setCellValue(product.getUrl());
                    row.createCell(1).setCellValue(product.getTitle());
                    row.createCell(2).setCellValue(product.getPrice());
                    row.createCell(3).setCellValue(product.getCurrency());
                    row.createCell(4).setCellValue(product.getCarModel());
                    row.createCell(5).setCellValue(product.getDescription());
                    row.createCell(7).setCellValue("default");
                }
            }

            // Zapisujemy plik Excel
            try (FileOutputStream fos = new FileOutputStream(FILE_NAME)) {
                workbook.write(fos);
                System.out.println("Dane zapisano do pliku Excel: " + FILE_NAME);
            }

            workbook.close();
        } catch (IOException e) {
            System.err.println("Błąd podczas zapisu do Excela: " + e.getMessage());
        }
    }

    private static void createHeaderRow(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("URL");
        header.createCell(1).setCellValue("Title");
        header.createCell(2).setCellValue("Price");
        header.createCell(3).setCellValue("Currency");
        header.createCell(4).setCellValue("Car Model");
        header.createCell(5).setCellValue("Description");
        header.createCell(6).setCellValue("Image Link");
        header.createCell(7).setCellValue("Variant Option");
    }
}
