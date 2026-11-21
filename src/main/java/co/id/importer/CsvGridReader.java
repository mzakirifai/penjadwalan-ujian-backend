package co.id.importer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Membaca file CSV (juga .txt hasil ekspor scanner) menjadi grid teks, sama bentuknya dengan
 * {@link ExcelGridReader}. Pemisah kolom (koma, titik koma, tab, atau |) dikenali otomatis,
 * tanda kutip dua didukung, BOM UTF-8 dibuang, dan file non-UTF-8 dibaca sebagai Windows-1252.
 */
public class CsvGridReader {

    public static List<List<String>> read(File file) throws IOException {
        return parse(decode(Files.readAllBytes(file.toPath())));
    }

    static String decode(byte[] bytes) {
        int offset = (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) ? 3 : 0;
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, offset, bytes.length - offset, java.nio.charset.Charset.forName("windows-1252"));
        }
    }

    static List<List<String>> parse(String text) {
        char delimiter = detectDelimiter(text);
        List<List<String>> grid = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == delimiter) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString());
                cell.setLength(0);
                grid.add(row);
                row = new ArrayList<>();
            } else {
                cell.append(ch);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            grid.add(row);
        }
        return grid;
    }

    // Pemisah = karakter yang paling konsisten muncul (di luar tanda kutip) pada 50 baris pertama yang berisi.
    // Baris judul atau keterangan di atas tabel (tanpa pemisah) tidak ikut menentukan, dan koma desimal
    // di dalam data (mis. 85,5 pada file berpemisah titik koma) tidak mengalahkan pemisah yang sebenarnya.
    private static char detectDelimiter(String text) {
        char[] candidates = {',', ';', '\t', '|'};
        List<int[]> lineCounts = new ArrayList<>();
        int[] counts = new int[candidates.length];
        boolean inQuotes = false;
        boolean hasContent = false;

        for (int i = 0; i < text.length() && lineCounts.size() < 50; i++) {
            char ch = text.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                hasContent = true;
            } else if (!inQuotes && (ch == '\n' || ch == '\r')) {
                if (hasContent) {
                    lineCounts.add(counts);
                    counts = new int[candidates.length];
                    hasContent = false;
                }
            } else if (!inQuotes) {
                hasContent = hasContent || !Character.isWhitespace(ch);
                for (int c = 0; c < candidates.length; c++) {
                    if (ch == candidates[c]) {
                        counts[c]++;
                    }
                }
            }
        }
        if (hasContent) {
            lineCounts.add(counts);
        }

        char best = ',';
        int bestLines = 0;
        int bestPerLine = 0;
        for (int c = 0; c < candidates.length; c++) {
            // Berapa baris yang punya jumlah pemisah yang sama (modus)? Hanya baris yang memuat pemisah itu.
            Map<Integer, Integer> frequency = new HashMap<>();
            for (int[] line : lineCounts) {
                if (line[c] > 0) {
                    frequency.merge(line[c], 1, Integer::sum);
                }
            }
            for (Map.Entry<Integer, Integer> entry : frequency.entrySet()) {
                if (entry.getValue() > bestLines || (entry.getValue() == bestLines && entry.getKey() > bestPerLine)) {
                    best = candidates[c];
                    bestLines = entry.getValue();
                    bestPerLine = entry.getKey();
                }
            }
        }
        return best;
    }
}