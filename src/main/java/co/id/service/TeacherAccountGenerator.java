package co.id.service;

import co.id.model.Teacher;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Usulan username dan password awal untuk membuat akun guru sekaligus.
 *
 * Username: nama depan guru dalam huruf kecil tanpa gelar (mis. "Drs. Agustinus Wibawa" -> "agustinus").
 * Bila sudah dipakai: nama depan + "." + nama kedua ("agus.hermansyah"), lalu ditambah angka ("agus2").
 * Password: 8 karakter acak tanpa huruf/angka yang mirip (l, 1, o, 0, i), mudah dibaca saat dibagikan.
 */
public final class TeacherAccountGenerator {

    public static final String USERNAME_PATTERN = "[a-z0-9._]{3,50}";
    private static final String PASSWORD_CHARS = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Sebutan/gelar di depan nama yang tidak dipakai sebagai username. */
    private static final Set<String> PREFIXES = Set.of(
            "dr", "drs", "dra", "h", "hj", "ir", "prof", "ust", "ustadz", "ustadzah", "ustazah", "bpk", "bapak", "ibu", "kh");

    private TeacherAccountGenerator() {
    }

    /**
     * @param taken username yang sudah dipakai (huruf kecil); hasilnya ikut ditambahkan ke sini
     *              supaya usulan berikutnya tidak bentrok
     */
    public static String suggestUsername(Teacher teacher, Set<String> taken) {
        List<String> words = nameWords(teacher.getName());
        String first = words.isEmpty() ? "guru" : words.get(0);
        if (first.length() < 3 && words.size() > 1) {
            first = first + words.get(1);
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(first);
        if (words.size() > 1 && !first.endsWith(words.get(1))) {
            candidates.add(first + "." + words.get(1));
        }
        for (String candidate : candidates) {
            String c = fit(candidate);
            if (c.length() >= 3 && taken.add(c)) {
                return c;
            }
        }
        String base = fit(first.length() >= 3 ? first : "guru");
        for (int i = 2; ; i++) {
            String c = fit(base.substring(0, Math.min(base.length(), 47)) + i);
            if (taken.add(c)) {
                return c;
            }
        }
    }

    public static String randomPassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /** Kata-kata nama tanpa gelar di belakang koma, gelar di depan, dan singkatan bertitik. */
    static List<String> nameWords(String name) {
        List<String> words = new ArrayList<>();
        if (name == null) {
            return words;
        }
        String main = name.split(",")[0];
        String plain = Normalizer.normalize(main, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        for (String raw : plain.trim().split("\\s+")) {
            if (raw.contains(".") && !words.isEmpty()) {
                break; // gelar tanpa koma, mis. "Lasiyo Sutanto S.Pd"
            }
            String w = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (w.isEmpty() || (words.isEmpty() && PREFIXES.contains(w))) {
                continue;
            }
            words.add(w);
        }
        return words;
    }

    private static String fit(String username) {
        String u = username.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._]", "");
        return u.length() > 50 ? u.substring(0, 50) : u;
    }
}