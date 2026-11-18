package co.id.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Membuat file template Excel (.xlsx): satu sheet data berisi judul kolom saja,
 * dan satu sheet "Petunjuk" berisi cara pengisian.
 * Semua kolom diformat sebagai Teks supaya angka panjang (NIP) dan nol di depan (No. HP) tidak rusak.
 */
public class ExcelTemplateWriter {

    public static void write(File file, String dataSheetName, List<String> headers, List<String> notes)
            throws IOException {
        write(file, dataSheetName, headers, notes, List.of());
    }

    /** Sama seperti di atas, dengan baris data yang sudah terisi (mis. daftar peserta ujian). */
    public static void write(File file, String dataSheetName, List<String> headers, List<String> notes,
                             List<List<String>> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {

            // gaya: kolom teks + judul tebal
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));

            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            headerStyle.setFont(bold);

            Sheet data = workbook.createSheet(dataSheetName);
            Row headerRow = data.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                data.setDefaultColumnStyle(i, textStyle);
                data.setColumnWidth(i, 22 * 256);
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < rows.size(); r++) {
                Row dataRow = data.createRow(r + 1);
                for (int c = 0; c < rows.get(r).size(); c++) {
                    org.apache.poi.ss.usermodel.Cell cell = dataRow.createCell(c);
                    cell.setCellValue(rows.get(r).get(c));
                    cell.setCellStyle(textStyle);
                }
            }

            Sheet guide = workbook.createSheet("Petunjuk");
            guide.setColumnWidth(0, 110 * 256);
            for (int i = 0; i < notes.size(); i++) {
                guide.createRow(i).createCell(0).setCellValue(notes.get(i));
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        }
    }
}