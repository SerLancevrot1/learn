package ru.learn.interview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "LAB_POSTGRES_URL", matches = ".+")
class PostgresConcurrencyLabTest {
    @Test
    void reproducesLostUpdateAndFixesItWithAnAtomicUpdate() throws SQLException {
        String schema = "interview_" + UUID.randomUUID().toString().replace("-", "");
        try (var setup = connect()) {
            execute(setup, "CREATE SCHEMA " + schema);
            try {
                setup.setSchema(schema);
                execute(setup, "CREATE TABLE counter (id INT PRIMARY KEY, value INT NOT NULL)");
                execute(setup, "INSERT INTO counter VALUES (1, 100)");

                try (var a = begin(schema); var b = begin(schema)) {
                    int fromA = value(a);
                    int fromB = value(b);
                    assertEquals(100, fromA);
                    assertEquals(100, fromB);
                    setValue(a, fromA + 10);
                    a.commit();
                    setValue(b, fromB + 20);
                    b.commit();
                }
                assertEquals(120, value(setup));
                setValue(setup, 100);

                try (var a = begin(schema); var b = begin(schema)) {
                    assertEquals(100, value(a));
                    assertEquals(100, value(b));
                    execute(a, "UPDATE counter SET value = value + 10 WHERE id = 1");
                    a.commit();
                    execute(b, "UPDATE counter SET value = value + 20 WHERE id = 1");
                    b.commit();
                }
                assertEquals(130, value(setup));
            } finally {
                // Схема создана только этим тестом. Таблицы исходного приложения не участвуют.
                execute(setup, "DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private static Connection connect() throws SQLException {
        var properties = new Properties();
        properties.setProperty("user", System.getenv().getOrDefault("LAB_POSTGRES_USER", "myuser"));
        properties.setProperty("password", System.getenv().getOrDefault("LAB_POSTGRES_PASSWORD", "secret"));
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "10");
        return DriverManager.getConnection(System.getenv("LAB_POSTGRES_URL"), properties);
    }

    private static Connection begin(String schema) throws SQLException {
        var connection = connect();
        try {
            connection.setSchema(schema);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    private static void setValue(Connection connection, int value) throws SQLException {
        try (var statement = connection.prepareStatement("UPDATE counter SET value = ? WHERE id = 1")) {
            statement.setInt(1, value);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static int value(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT value FROM counter WHERE id = 1")) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
