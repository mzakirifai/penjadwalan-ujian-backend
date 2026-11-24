package co.id.importer;

import co.id.model.Teacher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mengubah baris Excel menjadi data Guru + validasi.
 *
 * Kolom Excel: NIP | Nama | Jenis Kelamin | No. HP | Email | Alamat
 * Wajib: Nama, Jenis Kelamin (kolom jenis_kelamin di database NOT NULL).
 * NIP opsional (banyak guru honorer belum punya NIP), tetapi jika diisi harus unik.
 * Nama yang sama (perbedaan gelar diabaikan, lihat {@link #baseName}) hanya dianggap guru yang sama bila salah satunya
 * tidak punya NIP, supaya file yang sama tidak menggandakan guru tanpa NIP bila diimpor dua kali.
 * Nama sama dengan NIP berbeda dianggap dua guru yang berbeda dan tetap diimpor.
 */
public class TeacherImportParser {

    public static final List<String> COLUMNS =
            List.of("NIP", "Nama", "Jenis Kelamin", "No. HP", "Email", "Alamat");
    public static final List<String> REQUIRED = List.of("name", "gender");
    public static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("nip", List.of("nip"));
        ALIASES.put("name", List.of("nama", "namaguru"));
        ALIASES.put("gender", List.of("jeniskelamin", "lp", "jk", "gender"));
        ALIASES.put("phone", List.of("nohp", "nomorhp", "hp", "telepon", "notelp", "notelepon"));
        ALIASES.put("email", List.of("email", "surel"));
        ALIASES.put("address", List.of("alamat"));
    }

    public static List<ImportItem<Teacher>> parse(List<TableParser.Row> rows, Collection<Teacher> existing) {
        Set<String> existingNip = new HashSet<>();
        Map<String, List<Teacher>> existingByName = new HashMap<>();
        for (Teacher t : existing) {
            if (t.getNip() != null) {
                existingNip.add(t.getNip().trim());
            }
            existingByName.computeIfAbsent(baseName(t.getName()), k -> new ArrayList<>()).add(t);
        }

        Set<String> seen = new HashSet<>();
        // nama (tanpa gelar) yang sudah muncul di file -> true bila salah satu barisnya tanpa NIP
        Map<String, Boolean> seenNames = new HashMap<>();
        List<ImportItem<Teacher>> result = new ArrayList<>();

        for (TableParser.Row row : rows) {
            String nip = row.get("nip").replaceAll("\\s+", "");
            String name = row.get("name");
            String gender = normalizeGender(row.get("gender"));
            String phone = cleanPhone(row.get("phone"));
            String email = row.get("email").replaceAll("\\s+", "").toLowerCase();
            String address = row.get("address");

            // Lewati baris yang jelas bukan data guru: baris kop/rekap/tanda tangan di bawah tabel
            // (mis. "Jumlah Guru : 3", "Kepala Sekolah"). Baris dengan nama tapi tanpa NIP tetap diproses (NIP opsional).
            boolean noDigitInNip = !nip.matches(".*\\d.*");
            if ((nip.isEmpty() && name.isEmpty())
                    || (noDigitInNip && row.get("gender").isEmpty() && phone.isEmpty() && email.isEmpty()
                        && address.isEmpty() && (name.isEmpty() || name.matches("\\d+")))) {
                continue;
            }

            String base = baseName(name);
            List<Teacher> sameName = existingByName.getOrDefault(base, List.of());
            Teacher sameNameWithoutNip = sameName.stream().filter(t -> t.getNip() == null || t.getNip().isBlank())
                    .findFirst().orElse(null);
            Boolean seenWithoutNip = seenNames.get(base);
            String warning = null;

            String error = null;
            if (!nip.isEmpty() && nip.matches("^\\d+(\\.\\d+)?[eE][+-]?\\d+$")) {
                error = "NIP terbaca sebagai angka ilmiah, ubah kolom NIP menjadi format Teks";
            } else if (nip.length() > 20) {
                error = "NIP lebih dari 20 karakter";
            } else if (name.isEmpty()) {
                error = "Nama kosong";
            } else if (name.length() > 100) {
                error = "Nama lebih dari 100 karakter";
            } else if (gender == null) {
                error = "Jenis kelamin harus L/P (atau Laki-laki/Perempuan)";
            } else if (phone.length() > 15) {
                error = "No. HP lebih dari 15 karakter";
            } else if (!email.isEmpty() && (email.length() > 100 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
                error = "Format email tidak valid";
            } else if (!nip.isEmpty() && existingNip.contains(nip)) {
                error = "NIP sudah terdaftar";
            } else if (!sameName.isEmpty() && nip.isEmpty()) {
                error = "Sudah terdaftar sebagai \"" + sameName.get(0).getName() + "\", dilewati";
            } else if (sameNameWithoutNip != null) {
                error = "Nama sama dengan guru tanpa NIP \"" + sameNameWithoutNip.getName()
                        + "\". Bila orang yang sama, isi NIP-nya lewat Edit di halaman Guru";
            } else if (!nip.isEmpty() && !seen.add(nip)) {
                error = "NIP ganda di file";
            } else if (seenWithoutNip != null && (seenWithoutNip || nip.isEmpty())) {
                error = "Nama guru ganda di file";
            } else if (!sameName.isEmpty() || seenWithoutNip != null) {
                warning = "nama sama dengan guru lain, NIP berbeda";
            }
            if (error == null) {
                seenNames.merge(base, nip.isEmpty(), Boolean::logicalOr);
            }

            Teacher teacher = new Teacher();
            teacher.setNip(nip.isEmpty() ? null : nip);
            teacher.setName(name);
            teacher.setGender(gender);
            teacher.setPhoneNumber(phone.isEmpty() ? null : phone);
            teacher.setEmail(email.isEmpty() ? null : email);
            teacher.setAddress(address.isEmpty() ? null : address);

            result.add(new ImportItem<>(row.getSheet(), row.getRowNumber(),
                    List.of(nip.isEmpty() ? "-" : nip, name, gender == null ? "-" : gender, phone, email, address),
                    teacher, error, warning));
        }
        return result;
    }

    /**
     * Merapikan nomor HP. Nomor yang diketik sebagai angka di Excel kehilangan angka 0 di depan
     * (085718029981 menjadi 85718029981), jadi nomor yang diawali 8 dikembalikan menjadi 08...
     * Awalan +62 / 62 juga diubah menjadi 0.
     */
    private static String cleanPhone(String value) {
        String p = value.replaceAll("[\\s\\-().]", "");
        if (p.startsWith("+62")) {
            p = "0" + p.substring(3);
        } else if (p.matches("^62\\d{8,12}$")) {
            p = "0" + p.substring(2);
        } else if (p.matches("^8\\d{8,12}$")) {
            p = "0" + p;
        }
        return p;
    }

    /**
     * Nama tanpa gelar untuk membandingkan guru: bagian sebelum koma, lalu kata yang mengandung titik
     * (S.Kom, Drs., A.Md) dibuang. "Widya Ningsih, A.Md" dan "Widya Ningsih, A.Md.I.Kom." sama-sama "widyaningsih".
     */
    public static String baseName(String name) {
        if (name == null) {
            return "";
        }
        String beforeComma = name.split(",", 2)[0];
        StringBuilder words = new StringBuilder();
        for (String word : beforeComma.trim().split("\\s+")) {
            if (!word.contains(".")) {
                words.append(word);
            }
        }
        String base = TableParser.normalize(words.toString());
        // nama yang seluruhnya bergelar (jarang) tetap dibandingkan utuh
        return base.isEmpty() ? TableParser.normalize(name) : base;
    }

    private static String normalizeGender(String value) {
        String v = value.trim().toLowerCase();
        if (v.equals("l") || v.startsWith("laki")) return "Laki-laki";
        if (v.equals("p") || v.startsWith("perempuan") || v.startsWith("wanita")) return "Perempuan";
        return null;
    }
}