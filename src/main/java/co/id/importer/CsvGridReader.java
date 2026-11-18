package co.id.importer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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

    // Pemisah = karakter yang paling sering muncul (di luar tanda kutip) pada baris pertama yang berisi
    private static char detectDelimiter(String text) {
        char[] candidates = {',', ';', '\t', '|'};
        int[] counts = new int[candidates.length];
        boolean inQuotes = false;
        boolean seenContent = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                seenContent = true;
            } else if (!inQuotes && (ch == '\n' || ch == '\r')) {
                if (seenContent) {
                    break;
                }
            } else if (!inQuotes) {
                seenContent = seenContent || !Character.isWhitespace(ch);
                for (int c = 0; c < candidates.length; c++) {
                    if (ch == candidates[c]) {
                        counts[c]++;
                    }
                }
            }
        }

        int best = 0;
        for (int c = 1; c < candidates.length; c++) {
            if (counts[c] > counts[best]) {
                best = c;
            }
        }
        return counts[best] == 0 ? ',' : candidates[best];
    }
}