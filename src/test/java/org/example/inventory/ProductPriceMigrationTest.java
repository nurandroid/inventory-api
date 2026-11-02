package org.example.inventory;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductPriceMigrationTest {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/inventory_test_db";
    private static final String USER = "postgres";

    private String password;
    private String schema;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        password = System.getenv("DB_PASSWORD");
        assertNotNull(password, "DB_PASSWORD must be set");

        connection = DriverManager.getConnection(URL, USER, password);
        assertEquals("inventory_test_db", connection.getCatalog());

        String newSchema =
                "price_migration_" + UUID.randomUUID().toString().replace("-", "");

        execute("CREATE SCHEMA " + newSchema);
        schema = newSchema;
        connection.setSchema(schema);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null) {
            try {
                if (schema != null) {
                    execute("DROP SCHEMA " + schema + " CASCADE");
                }
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void migrate_shouldCreateRequiredPriceOnEmptyDatabase() throws SQLException {
        migrateTo("3");

        try (var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT is_nullable, numeric_precision, numeric_scale
                     FROM information_schema.columns
                     WHERE table_schema = current_schema()
                       AND table_name = 'products'
                       AND column_name = 'price'
                     """)) {

            assertTrue(result.next());
            assertEquals("NO", result.getString("is_nullable"));
            assertEquals(12, result.getInt("numeric_precision"));
            assertEquals(2, result.getInt("numeric_scale"));
        }

        SQLException exception = assertThrows(SQLException.class, () ->
                execute("""
                        INSERT INTO products (name, quantity)
                        VALUES ('Keyboard', 10)
                        """)
        );

        assertEquals("23502", exception.getSQLState());
    }

    @Test
    void migrate_shouldPreserveProductsAndPreparedPrices() throws SQLException {
        prepareOldProducts();
        prepareApprovedPrices();

        migrateTo("3");

        assertProductsAndPrices();
    }

    @Test
    void migrate_shouldRollbackWhenPricesAreMissingAndAllowRetry()
            throws SQLException {

        prepareOldProducts();

        FlywayException exception = assertThrows(
                FlywayException.class,
                () -> migrateTo("3")
        );

        assertTrue(exception.getMessage()
                .contains("Product prices are missing or invalid"));

        try (var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM information_schema.columns
                     WHERE table_schema = current_schema()
                       AND table_name = 'products'
                       AND column_name = 'price'
                     """)) {

            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }

        prepareApprovedPrices();
        migrateTo("3");

        assertProductsAndPrices();
    }

    @Test
    void migrate_shouldRejectNonPositivePrice() throws SQLException {
        prepareOldProducts();
        prepareApprovedPrices();
        execute("UPDATE products SET price = 0 WHERE id = 2");

        FlywayException exception = assertThrows(
                FlywayException.class,
                () -> migrateTo("3")
        );

        assertTrue(exception.getMessage()
                .contains("Product prices are missing or invalid"));
    }

    @Test
    void migrateV4_shouldPreserveExistingProducts() throws SQLException {
        prepareOldProducts();
        prepareApprovedPrices();
        migrateTo("3");

        migrateTo("4");

        assertProductsAndPrices();
    }

    @Test
    void migrateV4_shouldEnforceProductConstraints() throws SQLException {
        migrateTo("4");

        execute("""
                INSERT INTO products (name, quantity, price)
                VALUES (repeat('A', 255), 0, 0.01)
                """);

        assertRejectedProduct("NULL", "0", "1.00", "23502");
        assertRejectedProduct("''", "0", "1.00", "23514");
        assertRejectedProduct("'   '", "0", "1.00", "23514");
        assertRejectedProduct("repeat('A', 256)", "0", "1.00", "22001");

        assertRejectedProduct("'Mouse'", "-1", "1.00", "23514");
        assertRejectedProduct("'Mouse'", "NULL", "1.00", "23502");

        assertRejectedProduct("'Mouse'", "0", "0.00", "23514");
        assertRejectedProduct("'Mouse'", "0", "-0.01", "23514");
        assertRejectedProduct("'Mouse'", "0", "NULL", "23502");

        try (var statement = connection.createStatement();
             var result = statement.executeQuery(
                     "SELECT COUNT(*) FROM products")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    private void assertRejectedProduct(
            String nameSql,
            String quantitySql,
            String priceSql,
            String expectedSqlState) {

        String sql = """
                INSERT INTO products (name, quantity, price)
                VALUES (%s, %s, %s)
                """.formatted(nameSql, quantitySql, priceSql);

        SQLException exception = assertThrows(
                SQLException.class,
                () -> execute(sql),
                sql
        );

        assertEquals(expectedSqlState, exception.getSQLState(), sql);
    }

    private void prepareOldProducts() throws SQLException {
        migrateTo("1");

        execute("""
                INSERT INTO products (id, name, quantity)
                VALUES
                    (1, 'Keyboard', 10),
                    (2, 'Mouse', 25)
                """);
    }

    private void prepareApprovedPrices() throws SQLException {
        // Test fixture representing prices supplied before the upgrade.
        execute("ALTER TABLE products ADD COLUMN price NUMERIC(12, 2)");
        execute("UPDATE products SET price = 45.00 WHERE id = 1");
        execute("UPDATE products SET price = 20.00 WHERE id = 2");
    }

    private void assertProductsAndPrices() throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT id, name, quantity, price
                     FROM products
                     ORDER BY id
                     """)) {

            assertTrue(result.next());
            assertEquals(1L, result.getLong("id"));
            assertEquals("Keyboard", result.getString("name"));
            assertEquals(10, result.getInt("quantity"));
            assertEquals(new BigDecimal("45.00"), result.getBigDecimal("price"));

            assertTrue(result.next());
            assertEquals(2L, result.getLong("id"));
            assertEquals("Mouse", result.getString("name"));
            assertEquals(25, result.getInt("quantity"));
            assertEquals(new BigDecimal("20.00"), result.getBigDecimal("price"));

            assertFalse(result.next());
        }
    }

    private void migrateTo(String version) {
        Flyway.configure()
                .dataSource(URL, USER, password)
                .defaultSchema(schema)
                .schemas(schema)
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    private void execute(String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}