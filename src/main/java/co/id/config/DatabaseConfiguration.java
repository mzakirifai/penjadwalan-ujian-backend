package co.id.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfiguration {
    private static HikariDataSource hikariDataSource;

    static {
        try (InputStream inputStream = DatabaseConfiguration.class.getClassLoader().getResourceAsStream("connection.properties")) {
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

            hikariDataSource = new HikariDataSource(hikariConfig);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    protected Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }
}