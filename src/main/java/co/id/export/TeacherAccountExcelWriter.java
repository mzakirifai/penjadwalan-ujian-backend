package co.id.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Daftar akun guru yang baru dibuat (username + password awal) untuk dibagikan ke masing-masing guru.
 * Password hanya bisa dilihat di file ini, karena di database password disimpan dalam bentuk acak (hash).
 */
public class TeacherAccountExcelWriter {

    public record Account(String teacherName, String nip, String username, String password) { }

    public static void write(File file, List<Account> accounts) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Akun Guru");

            Font bold = wb.createFont();
            bold.setBold(true);
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);

            CellStyle title = wb.createCellStyle();
            title.setFont(titleFont);
            CellStyle header = bordered(wb);
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle cell = bordered(wb);
            CellStyle mono = bordered(wb);
            Font monoFont = wb.createFont();
            monoFont.setFontName("Consolas");
            monoFont.setBold(true);
            mono.setFont(monoFont);
            mono.setDataFormat(wb.createDataFormat().getFormat("@"));

            int r = 0;
            set(sheet.createRow(r), 0, "DAFTAR AKUN GURU - SISTEM PENJADWALAN UJIAN", title);
            sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 4));
            r++;
            set(sheet.createRow(r++), 0, "Dibuat: " + LocalDate.now().format(
                    DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"))), null);
            set(sheet.createRow(r++), 0, "RAHASIA - bagikan baris milik masing-masing guru saja. "
                    + "Setelah login pertama, segera ganti password lewat menu Pengaturan Akun > Ganti Password.", null);
            r++;

            String[] headers = {"No", "Nama Guru", "NIP", "Username", "Password Awal"};
            Row h = sheet.createRow(r++);
            for (int c = 0; c < headers.length; c++) {
                set(h, c, headers[c], header);
            }
            int no = 1;
            for (Account a : accounts) {
                Row row = sheet.createRow(r++);
                set(row, 0, String.valueOf(no++), cell);
                set(row, 1, a.teacherName(), cell);
                set(row, 2, a.nip() == null ? "" : a.nip(), cell);
                set(row, 3, a.username(), mono);
                set(row, 4, a.password(), mono);
            }

            int[] widths = {6, 40, 22, 22, 18};
            for (int c = 0; c < widths.length; c++) {
                sheet.setColumnWidth(c, widths[c] * 256);
            }
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitHeight((short) 0);

            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }
    }

    private static CellStyle bordered(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static void set(Row row, int column, String value, CellStyle style) {
        Cell c = row.createCell(column);
        c.setCellValue(value);
        if (style != null) {
            c.setCellStyle(style);
        }
    }
}