package com.marine.management.modules.files;

import com.marine.management.modules.files.ExcelRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ExcelParserService {

    private static final int COL_DATE = 0;        // A - Tarih (01.01.22 formatında)
    private static final int COL_TYPE = 1;        // B - İşlem Türü/Açıklama
    private static final int COL_CATEGORY = 2;    // C - Kategori
    private static final int COL_EXPENSE = 3;     // D - Gider Tutarı
    private static final int COL_INCOME = 4;      // E - Gelir Tutarı
    private static final int COL_DESCRIPTION = 5; // F - Açıklama

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("([\\d.,]+)\\s*€?");
    private static final Pattern CLEAN_AMOUNT_PATTERN = Pattern.compile("[^\\d.,-]");

    public List<ExcelRow> parseExcel(MultipartFile file) throws IOException {
        validateFile(file);

        List<ExcelRow> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Başlık satırını atla (row 0) ve veri satırlarını işle
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                try {
                    ExcelRow excelRow = parseRow(row, i + 1); // +1 because Excel rows are 1-indexed
                    if (excelRow != null) {
                        rows.add(excelRow);
                    }
                } catch (Exception e) {
                    System.err.println("Satır " + (i + 1) + " parse edilemedi: " + e.getMessage());
                }
            }
        }

        return rows;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new IllegalArgumentException("Dosya Excel formatında olmalı (.xlsx veya .xls)");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Dosya boyutu 10MB'tan küçük olmalı");
        }
    }

    private ExcelRow parseRow(Row row, int rowNumber) {
        try {
            LocalDate date = parseDate(row.getCell(COL_DATE), rowNumber);
            String type = parseString(row.getCell(COL_TYPE));
            String category = parseString(row.getCell(COL_CATEGORY));
            String description = parseString(row.getCell(COL_DESCRIPTION));

            // Gelir ve gider tutarlarını parse et
            BigDecimal expenseAmount = parseAmountWithCurrency(row.getCell(COL_EXPENSE));
            BigDecimal incomeAmount = parseAmountWithCurrency(row.getCell(COL_INCOME));

            // Gelir mi gider mi olduğunu belirle
            boolean isIncome = incomeAmount != null && incomeAmount.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal amount = isIncome ? incomeAmount : expenseAmount;

            // Eğer her iki tutar da null veya 0 ise bu satırı atla
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }

            // Zorunlu alanları kontrol et
            if (date == null) {
                throw new IllegalArgumentException("Tarih alanı boş olamaz");
            }
            if (type == null || type.trim().isEmpty()) {
                throw new IllegalArgumentException("İşlem türü boş olamaz");
            }
            if (category == null || category.trim().isEmpty()) {
                throw new IllegalArgumentException("Kategori boş olamaz");
            }

            // Para birimi olarak EUR kullan
            String currency = "EUR";

            return new ExcelRow(date, type, category, amount, currency, description, isIncome);

        } catch (Exception e) {
            throw new RuntimeException("Satır " + rowNumber + " işlenirken hata: " + e.getMessage(), e);
        }
    }

    private LocalDate parseDate(Cell cell, int rowNumber) {
        if (cell == null) {
            throw new IllegalArgumentException("Tarih hücresi boş");
        }

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                    } else {
                        // Sayısal değeri tarihe çevir (Excel tarih numarası)
                        return DateUtil.getJavaDate(cell.getNumericCellValue())
                                .toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                    }

                case STRING:
                    String dateStr = cell.getStringCellValue().trim();
                    if (dateStr.isEmpty()) {
                        throw new IllegalArgumentException("Tarih değeri boş");
                    }

                    // "01.01.22" formatını parse et
                    if (dateStr.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}")) {
                        return LocalDate.parse(dateStr, DATE_FORMATTER);
                    } else {
                        throw new IllegalArgumentException("Geçersiz tarih formatı: " + dateStr);
                    }

                default:
                    throw new IllegalArgumentException("Desteklenmeyen tarih formatı: " + cell.getCellType());
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Tarih parse edilemedi: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("Tarih işlenirken hata: " + e.getMessage());
        }
    }

    private String parseString(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();

                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // Tam sayı ise int, değilse double olarak formatla
                        double value = cell.getNumericCellValue();
                        if (value == Math.floor(value)) {
                            return String.valueOf((int) value);
                        } else {
                            return String.valueOf(value);
                        }
                    }

                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());

                case FORMULA:
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        try {
                            return String.valueOf(cell.getNumericCellValue());
                        } catch (Exception e2) {
                            return cell.getCellFormula();
                        }
                    }

                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("String parse hatası: " + e.getMessage());
            return null;
        }
    }

    private BigDecimal parseAmountWithCurrency(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());

                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) {
                        return null;
                    }

                    // "157 €", "1.500 €", "35.000 €" formatlarını işle
                    value = CLEAN_AMOUNT_PATTERN.matcher(value).replaceAll("").trim();

                    // Noktaları binlik ayracı olarak kabul et ve kaldır
                    value = value.replace(".", "");
                    // Virgülü ondalık ayracı olarak kabul et ve noktaya çevir
                    value = value.replace(",", ".");

                    if (value.isEmpty()) {
                        return null;
                    }

                    return new BigDecimal(value);

                case FORMULA:
                    try {
                        return BigDecimal.valueOf(cell.getNumericCellValue());
                    } catch (Exception e) {
                        return null;
                    }

                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Tutar parse hatası: " + e.getMessage());
            return null;
        }
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i <= COL_DESCRIPTION; i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                String value = parseString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}