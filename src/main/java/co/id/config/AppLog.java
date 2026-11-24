package co.id.config;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Pencatat error aplikasi (pengganti printStackTrace).
 *
 * Setiap error dicatat beserta lokasinya (Class.method) dan detail teknisnya ke:
 *  - konsol/stderr (oleh run.bat disimpan ke %TEMP%\penjadwalan-ujian-log.txt, ditimpa tiap aplikasi dibuka);
 *  - file log permanen yang bergiliran di folder pengguna: %USERPROFILE%\.penjadwalan-ujian\logs\app0.log
 *    (maks. 1 MB x 3 file), supaya masalah yang terjadi kemarin masih bisa dicek.
 * Bila folder log tidak bisa dibuat, pencatatan ke konsol tetap berjalan.
 */
public final class AppLog {

    private static final Logger LOG = Logger.getLogger("PenjadwalanUjian");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final File LOG_DIR = new File(System.getProperty("user.home"), ".penjadwalan-ujian" + File.separator + "logs");

    static {
        LOG.setUseParentHandlers(false);
        Formatter formatter = new Formatter() {
            @Override
            public String format(LogRecord record) {
                StringBuilder sb = new StringBuilder()
                        .append(LocalDateTime.now().format(TIME)).append(' ')
                        .append(record.getLevel() == Level.SEVERE ? "ERROR" : record.getLevel().getName()).append(' ')
                        .append(record.getMessage()).append(System.lineSeparator());
                if (record.getThrown() != null) {
                    java.io.StringWriter trace = new java.io.StringWriter();
                    record.getThrown().printStackTrace(new java.io.PrintWriter(trace));
                    sb.append(trace);
                }
                return sb.toString();
            }
        };
        Handler console = new ConsoleHandler();
        console.setFormatter(formatter);
        LOG.addHandler(console);
        try {
            if (LOG_DIR.isDirectory() || LOG_DIR.mkdirs()) {
                FileHandler file = new FileHandler(new File(LOG_DIR, "app%g.log").getPath(), 1_000_000, 3, true);
                file.setEncoding("UTF-8");
                file.setFormatter(formatter);
                LOG.addHandler(file);
            }
        } catch (Exception | Error ignored) {
            // tanpa file log: tetap tercatat di konsol
        }
    }

    private AppLog() {
    }

    /** Folder file log, untuk ditampilkan ke pengguna saat terjadi error. */
    public static String logFolder() {
        return LOG_DIR.getPath();
    }

    /** Catat error. @param where lokasi kejadian, mis. "StudentDAOImpl.getAllStudents" */
    public static void error(String where, Throwable error) {
        LOG.log(Level.SEVERE, where + ": " + (error == null ? "-" : error.getMessage()), error);
    }

    /** Catat kejadian yang tidak menghentikan proses (mis. data pelengkap gagal dibaca). */
    public static void warn(String where, Throwable error) {
        LOG.log(Level.WARNING, where + ": " + (error == null ? "-" : error.getMessage()), error);
    }
}