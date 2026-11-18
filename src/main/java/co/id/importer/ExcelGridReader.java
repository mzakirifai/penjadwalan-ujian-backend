package co.id.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Membaca semua sheet sebuah file Excel menjadi grid teks (apa adanya seperti tampilan di Excel).
 * Sheet bernama "Petunjuk" (lembar panduan di template) dilewati.
 */
public class ExcelGridReader {

    public static Map<String, List<List<String>>> readAll(File file) throws IOException {
        Map<String, List<List<String>>> result = new LinkedHashMap<>();

        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
            result.put("CSV", CsvGridReader.read(file));
            return result;
        }

        try (FileInputStream inputStream = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if ("petunjuk".equalsIgnoreCase(sheet.getSheetName().trim())) {
                    continue;
                }

                List<List<String>> grid = new ArrayList<>();
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    List<String> values = new ArrayList<>();

                    if (row != null) {
                        int lastCell = row.getLastCellNum(); // -1 jika baris kosong
                        for (int c = 0; c < lastCell; c++) {
                            Cell cell = row.getCell(c);
                            values.add(cell == null ? "" : formatter.formatCellValue(cell, evaluator));
                        }
                    }
                    grid.add(values);
                }
                result.put(sheet.getSheetName(), grid);
            }
        }
        return result;
    }
}