package co.id.importer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Menulis template impor dalam bentuk CSV.
 * Pemisah kolom titik koma (;) dan diawali BOM UTF-8, supaya langsung terbuka rapi per kolom
 * di Excel berbahasa Indonesia dan huruf seperti "é" tidak rusak. {@link CsvGridReader} mengenali
 * pemisah ; , tab, maupun | secara otomatis, jadi file hasil edit tetap bisa diimpor.
 */
public class CsvTemplateWriter {

    public static void write(File file, List<String> headers, List<List<String>> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(line(headers));
        for (List<String> row : rows) {
            lines.add(line(row));
        }
        String text = "﻿" + String.join("\r\n", lines) + "\r\n";
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static String line(List<String> cells) {
        List<String> out = new ArrayList<>();
        for (String cell : cells) {
            String v = cell == null ? "" : cell;
            if (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r") || v.contains(",")) {
                v = "\"" + v.replace("\"", "\"\"") + "\"";
            }
            out.add(v);
        }
        return String.join(";", out);
    }
}