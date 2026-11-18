package co.id.importer;

import java.util.List;

/**
 * Satu baris hasil baca Excel beserta hasil validasinya.
 * error == null artinya baris siap diimport.
 */
public class ImportItem<T> {
    private final String sheet;
    private final int rowNumber;
    private final List<String> cells;   // nilai yang ditampilkan di tabel pratinjau
    private final T payload;            // objek yang akan disimpan (Teacher, Subject, ...)
    private final String error;
    private final String warning;       // catatan untuk baris yang tetap diimport (boleh null)

    public ImportItem(String sheet, int rowNumber, List<String> cells, T payload, String error) {
        this(sheet, rowNumber, cells, payload, error, null);
    }

    public ImportItem(String sheet, int rowNumber, List<String> cells, T payload, String error, String warning) {
        this.warning = warning;
        this.sheet = sheet;
        this.rowNumber = rowNumber;
        this.cells = cells;
        this.payload = payload;
        this.error = error;
    }

    public String getSheet() { return sheet; }
    public int getRowNumber() { return rowNumber; }
    public List<String> getCells() { return cells; }
    public T getPayload() { return payload; }
    public String getError() { return error; }
    public String getWarning() { return warning; }
    public boolean isValid() { return error == null; }
}