package co.id.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Membaca file Excel (.xlsx / .xls) dan menghasilkan daftar siswa.
 *
 * Aturan pemilihan sheet:
 * - Jika ada sheet bernama X, XI, atau XII, hanya sheet itu yang dibaca
 *   (sheet lain seperti "Sheet1" yang berisi rekap/coretan diabaikan).
 * - Jika tidak ada, semua sheet dibaca.
 */
public class StudentExcelReader {

    public static class Result {
        private final List<StudentImportRow> rows = new ArrayList<>();
        private final List<String> sheetsRead = new ArrayList<>();

        public List<StudentImportRow> getRows() { return rows; }
        public List<String> getSheetsRead() { return sheetsRead; }
    }

    public static Result read(File file) throws IOException {
        Result result = new Result();

        try (FileInputStream inputStream = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            boolean hasGradeSheet = false;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (StudentSheetParser.gradeFromSheetName(workbook.getSheetName(i)) != null) {
                    hasGradeSheet = true;
                    break;
                }
            }

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (hasGradeSheet && StudentSheetParser.gradeFromSheetName(sheet.getSheetName()) == null) {
                    continue;
                }

                List<StudentImportRow> rows = StudentSheetParser.parse(
                        sheet.getSheetName(), toGrid(sheet, formatter, evaluator));

                if (!rows.isEmpty()) {
                    result.sheetsRead.add(sheet.getSheetName());
                    result.rows.addAll(rows);
                }
            }
        }
        return result;
    }

    /** Menyalin sheet menjadi grid teks (apa adanya seperti tampilan di Excel). */
    private static List<List<String>> toGrid(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<List<String>> grid = new ArrayList<>();
        int lastRow = sheet.getLastRowNum();

        for (int r = 0; r <= lastRow; r++) {
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
        return grid;
    }
}