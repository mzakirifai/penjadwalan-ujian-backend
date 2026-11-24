package co.id.export;

import co.id.model.ExamSchedule;
import co.id.model.RoomAssignment;
import co.id.model.RoomPlan;
import co.id.model.SeatAssignment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Daftar hadir per ruangan per kelas (satu sheet untuk tiap kelas di tiap ruangan, urut nama siswa;
 * ruangan yang berisi campuran kelas mendapat beberapa sheet) dan satu sheet ringkasan:
 * isi tiap ruangan serta naskah soal per kelas.
 */
public class RoomAttendanceExcelWriter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));

    public static void write(File file, RoomPlan plan) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            writeSummary(workbook, styles, plan);

            Set<String> usedNames = new HashSet<>();
            usedNames.add("Ringkasan");
            for (RoomAssignment room : plan.getRooms()) {
                for (Map.Entry<String, java.util.List<SeatAssignment>> group : seatsByClass(room).entrySet()) {
                    writeRoom(workbook, styles, plan, room, group.getKey(), group.getValue(),
                            uniqueSheetName(room.getRoom().getName() + " " + group.getKey(), usedNames));
                }
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        }
    }

    private static void writeSummary(Workbook workbook, Styles s, RoomPlan plan) {
        Sheet sheet = workbook.createSheet("Ringkasan");
        int r = 0;
        r = title(sheet, s, r, "PENEMPATAN RUANGAN UJIAN", 5);
        r = line(sheet, s, r, "Hari/Tanggal", dateText(plan));
        r = line(sheet, s, r, "Waktu", plan.getStartTime().format(TIME) + " - " + plan.getEndTime().format(TIME));
        r++;

        String[] headers = {"Ruangan", "Pengawas", "Kapasitas", "Terisi", "Kelas (jumlah siswa)"};
        Row header = sheet.createRow(r++);
        for (int c = 0; c < headers.length; c++) {
            cell(header, c, headers[c], s.header);
        }
        for (RoomAssignment room : plan.getRooms()) {
            Row row = sheet.createRow(r++);
            cell(row, 0, room.getRoom().getName(), s.cell);
            cell(row, 1, room.getSupervisor() != null ? room.getSupervisor().getName() : "-", s.cell);
            cell(row, 2, String.valueOf(room.getRoom().getCapacity()), s.center);
            cell(row, 3, String.valueOf(room.getFilled()), s.center);
            cell(row, 4, room.describeClasses(), s.cell);
        }

        r++;
        Row naskah = sheet.createRow(r++);
        cell(naskah, 0, "Naskah soal per kelas", s.bold);
        Row nh = sheet.createRow(r++);
        cell(nh, 0, "Kelas", s.header);
        cell(nh, 1, "Mata pelajaran", s.header);
        cell(nh, 2, "Siswa", s.header);

        Map<ExamSchedule, Integer> count = new LinkedHashMap<>();
        for (RoomAssignment room : plan.getRooms()) {
            for (SeatAssignment seat : room.getSeats()) {
                count.merge(seat.getSchedule(), 1, Integer::sum);
            }
        }
        for (Map.Entry<ExamSchedule, Integer> entry : count.entrySet()) {
            Row row = sheet.createRow(r++);
            cell(row, 0, entry.getKey().getClassroom() != null ? entry.getKey().getClassroom().getName() : "-", s.cell);
            cell(row, 1, entry.getKey().getSubjectLabel(), s.cell);
            cell(row, 2, String.valueOf(entry.getValue()), s.center);
        }

        int[] widths = {22, 28, 12, 10, 60};
        for (int c = 0; c < widths.length; c++) {
            sheet.setColumnWidth(c, widths[c] * 256);
        }
    }

    /** Kursi di satu ruangan dikelompokkan per kelas (urut nama kelas), di dalamnya urut nama siswa. */
    private static Map<String, java.util.List<SeatAssignment>> seatsByClass(RoomAssignment room) {
        Map<String, java.util.List<SeatAssignment>> byClass = new java.util.TreeMap<>();
        for (SeatAssignment seat : room.getSeats()) {
            byClass.computeIfAbsent(seat.getClassName(), k -> new java.util.ArrayList<>()).add(seat);
        }
        for (java.util.List<SeatAssignment> seats : byClass.values()) {
            seats.sort(java.util.Comparator.comparing(
                    (SeatAssignment seat) -> seat.getStudent().getName() == null ? "" : seat.getStudent().getName(),
                    String.CASE_INSENSITIVE_ORDER).thenComparing(SeatAssignment::getSeatIndex));
        }
        return byClass;
    }

    private static void writeRoom(Workbook workbook, Styles s, RoomPlan plan, RoomAssignment room,
                                  String className, java.util.List<SeatAssignment> seats, String sheetName) {
        Sheet sheet = workbook.createSheet(sheetName);
        int r = 0;
        r = title(sheet, s, r, "DAFTAR HADIR PESERTA UJIAN", 7);
        r = line(sheet, s, r, "Hari/Tanggal", dateText(plan));
        r = line(sheet, s, r, "Waktu", plan.getStartTime().format(TIME) + " - " + plan.getEndTime().format(TIME));
        r = line(sheet, s, r, "Ruangan", room.getRoom().getName() + " (kapasitas " + room.getRoom().getCapacity() + ")");
        r = line(sheet, s, r, "Pengawas", room.getSupervisor() != null ? room.getSupervisor().getName() : "-");
        ExamSchedule schedule = seats.get(0).getSchedule();
        r = line(sheet, s, r, "Kelas", className
                + (schedule != null && schedule.getSubject() != null ? " - " + schedule.getSubjectLabel() : ""));
        r++;

        String[] headers = {"No", "No. Kursi", "No. Peserta", "NIS", "Nama", "Kelas", "Tanda Tangan"};
        Row header = sheet.createRow(r++);
        for (int c = 0; c < headers.length; c++) {
            cell(header, c, headers[c], s.header);
        }

        int no = 1;
        for (SeatAssignment seat : seats) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(22);
            cell(row, 0, String.valueOf(no++), s.center);
            cell(row, 1, seat.getSeatLabel(), s.center);
            cell(row, 2, seat.getParticipantNumber() == null ? "" : seat.getParticipantNumber(), s.center);
            cell(row, 3, seat.getStudent().getNis() == null ? "" : seat.getStudent().getNis(), s.cell);
            cell(row, 4, seat.getStudent().getName() == null ? "" : seat.getStudent().getName(), s.cell);
            cell(row, 5, seat.getClassName(), s.cell);
            cell(row, 6, "", s.cell);
        }

        r++;
        r = line(sheet, s, r, "Jumlah peserta", String.valueOf(seats.size()));
        r = line(sheet, s, r, "Hadir / Tidak hadir", "........ / ........");
        r++;
        Row sign = sheet.createRow(r++);
        cell(sign, 4, "Pengawas,", s.plain);
        r += 2;
        Row name = sheet.createRow(r);
        cell(name, 4, room.getSupervisor() != null ? "( " + room.getSupervisor().getName() + " )" : "( ........................ )", s.plain);

        int[] widths = {6, 11, 13, 16, 34, 16, 24};
        for (int c = 0; c < widths.length; c++) {
            sheet.setColumnWidth(c, widths[c] * 256);
        }
        sheet.getPrintSetup().setLandscape(false);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    private static String dateText(RoomPlan plan) {
        return plan.getDate().getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("id", "ID"))
                + ", " + plan.getDate().format(DATE);
    }

    private static int title(Sheet sheet, Styles s, int r, String text, int columns) {
        Row row = sheet.createRow(r);
        cell(row, 0, text, s.title);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, columns - 1));
        return r + 2;
    }

    private static int line(Sheet sheet, Styles s, int r, String label, String value) {
        Row row = sheet.createRow(r);
        cell(row, 0, label, s.bold);
        cell(row, 2, ": " + value, s.plain);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 1));
        return r + 1;
    }

    private static void cell(Row row, int column, String value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static String uniqueSheetName(String raw, Set<String> used) {
        String base = raw.replaceAll("[\\\\/?*\\[\\]:]", "-").trim();
        if (base.isEmpty()) {
            base = "Ruangan";
        }
        if (base.length() > 28) {
            base = base.substring(0, 28);
        }
        String name = base;
        int i = 2;
        while (!used.add(name)) {
            name = base + " " + i++;
        }
        return name;
    }

    private static class Styles {
        final CellStyle title, header, cell, center, bold, plain;

        Styles(Workbook wb) {
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            title = wb.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);

            header = border(wb.createCellStyle());
            header.setFont(boldFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            cell = border(wb.createCellStyle());
            center = border(wb.createCellStyle());
            center.setAlignment(HorizontalAlignment.CENTER);

            bold = wb.createCellStyle();
            bold.setFont(boldFont);
            plain = wb.createCellStyle();
        }

        private static CellStyle border(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}