package co.id.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatabaseConfiguration {
    private static HikariDataSource hikariDataSource;
    private static String serverAddress = "-";

    static {
        try (InputStream inputStream = openConfig()) {
            Properties properties = new Properties();
            properties.load(inputStream);

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(properties.getProperty("jdbc.driver"));
            hikariConfig.setJdbcUrl(properties.getProperty("jdbc.url"));
            hikariConfig.setUsername(properties.getProperty("jdbc.username"));
            hikariConfig.setPassword(properties.getProperty("jdbc.password"));
            hikariConfig.setConnectionTimeout(Long.parseLong(properties.getProperty("jdbc.pool.timeout")));
            hikariConfig.setMaximumPoolSize(Integer.parseInt(properties.getProperty("jdbc.pool.max")));
            hikariConfig.setMinimumIdle(Integer.parseInt(properties.getProperty("jdbc.pool.min")));
            // Aplikasi tetap terbuka walau server database belum menyala/terjangkau saat dijalankan;
            // koneksi dicoba lagi setiap kali dibutuhkan (mis. saat login), tidak perlu menutup aplikasi.
            hikariConfig.setInitializationFailTimeout(-1);
            serverAddress = hostOf(properties.getProperty("jdbc.url"));

            hikariDataSource = new HikariDataSource(hikariConfig);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Cek koneksi ke server database.
     * @return null bila berhasil, atau pesan yang bisa langsung ditampilkan ke pengguna.
     */
    public static String checkConnection() {
        if (hikariDataSource == null) {
            return "Pengaturan koneksi database (config\\connection.properties) tidak bisa dibaca.";
        }
        try (Connection connection = hikariDataSource.getConnection()) {
            return null;
        } catch (SQLException exception) {
            return "Tidak bisa terhubung ke server database (" + serverAddress + "). "
                    + "Pastikan komputer server menyala, XAMPP/MySQL sudah berjalan, dan komputer ini satu jaringan dengan server.";
        }
    }

    /** Mengambil host:port dari jdbc.url, mis. jdbc:mysql://192.168.1.10:3306/db -> 192.168.1.10:3306 */
    private static String hostOf(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.contains("//")) {
            return "-";
        }
        String rest = jdbcUrl.substring(jdbcUrl.indexOf("//") + 2);
        int slash = rest.indexOf('/');
        return slash >= 0 ? rest.substring(0, slash) : rest;
    }

    protected Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }
    
    private static InputStream openConfig() throws IOException {
        String path = System.getProperty("app.config", "config/connection.properties");
        if (Files.exists(Path.of(path))) {
            return Files.newInputStream(Path.of(path));
        }
        return DatabaseConfiguration.class.getClassLoader().getResourceAsStream("connection.properties");
    }   
}