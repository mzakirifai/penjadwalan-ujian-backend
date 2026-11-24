package co.id.importer;

import co.id.model.Teacher;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Mencocokkan nama guru yang ditulis bebas di file (mis. "Agustri Wijaya, S.PdI", "Drs.Agustinus Wibawa")
 * dengan data Guru di aplikasi. Gelar, titik, spasi, dan huruf besar/kecil diabaikan.
 *
 *  - COCOK : nama sama persis setelah dirapikan ("agustriwijaya" = "agustriwijaya").
 *  - MIRIP : beda sedikit ejaan ("Nafsiah" vs "Napsiah"), atau satu nama adalah awal nama lain
 *            ("Farissa Rahmadani" vs "Farissa") -> perlu dicek pengguna.
 *  - TIDAK : tidak ada yang cukup mirip -> pengguna memilih sendiri.
 */
public final class TeacherNameMatcher {

    public enum Level { COCOK, MIRIP, TIDAK }

    public record Match(Teacher teacher, Level level) { }

    private static final Set<String> PREFIXES = Set.of("dr", "drs", "dra", "h", "hj", "ir", "prof", "kh",
            "ust", "ustadz", "ustadzah", "bpk", "bapak", "ibu", "bu", "pak");
    private static final double SIMILAR = 0.80;

    private TeacherNameMatcher() {
    }

    public static Match match(String fileName, Collection<Teacher> teachers) {
        String key = key(fileName);
        if (key.isEmpty()) {
            return new Match(null, Level.TIDAK);
        }
        Teacher exact = null;
        int exactCount = 0;
        Teacher best = null;
        double bestScore = 0;
        int bestCount = 0;
        for (Teacher t : teachers) {
            String k = key(t.getName());
            if (k.isEmpty()) {
                continue;
            }
            if (k.equals(key)) {
                exact = t;
                exactCount++;
                continue;
            }
            double score = similarity(key, k);
            boolean prefix = Math.min(k.length(), key.length()) >= 5 && (k.startsWith(key) || key.startsWith(k));
            if (prefix) {
                score = Math.max(score, SIMILAR);
            }
            if (score > bestScore + 1e-9) {
                best = t;
                bestScore = score;
                bestCount = 1;
            } else if (Math.abs(score - bestScore) < 1e-9) {
                bestCount++;
            }
        }
        if (exactCount == 1) {
            return new Match(exact, Level.COCOK);
        }
        if (exactCount == 0 && best != null && bestScore >= SIMILAR && bestCount == 1) {
            return new Match(best, Level.MIRIP);
        }
        return new Match(null, Level.TIDAK);
    }

    /** "Drs. Agustinus Wibawa" -> "agustinuswibawa"; "Lasiyo Sutanto S.Pd" -> "lasiyosutanto". */
    static String key(String name) {
        if (name == null) {
            return "";
        }
        String main = name.split(",")[0];
        String plain = Normalizer.normalize(main, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        // "Drs.Agustinus" -> "Drs. Agustinus"
        plain = plain.replaceAll("(?i)^(drs|dra|dr|hj|h|ir|prof)\\.(?=\\S)", "$1. ");
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String raw : plain.trim().split("\\s+")) {
            String w = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            if (w.isEmpty()) {
                continue;
            }
            if (first && PREFIXES.contains(w)) {
                continue;
            }
            if (!first && raw.contains(".")) {
                break; // gelar tanpa koma, mis. "Lasiyo Sutanto S.Pd"
            }
            sb.append(w);
            first = false;
        }
        return sb.toString();
    }

    /** 1 - (jarak Levenshtein / panjang terpanjang). */
    static double similarity(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return 1.0 - (double) prev[b.length()] / Math.max(a.length(), b.length());
    }
}