package ru.learn.interview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(ProxyTransactionLabTest.Config.class)
class ProxyTransactionLabTest {
    @Autowired
    private OrderWriter writer;

    @Autowired
    private Checkout checkout;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetOrders() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS orders (id INT PRIMARY KEY)");
        jdbc.update("DELETE FROM orders");
    }

    @Test
    void selfInvocationCommitsBeforeTheException() {
        assertThrows(PaymentRejected.class, () -> writer.placeFromSameBean(1));
        assertEquals(1, orderCount());
    }

    @Test
    void callingAnotherBeanRollsBackTheInsert() {
        assertThrows(PaymentRejected.class, () -> checkout.place(1));
        assertEquals(0, orderCount());
    }

    @Test
    void catchingRequiredFailureDoesNotClearRollbackOnly() {
        assertThrows(UnexpectedRollbackException.class, () -> checkout.catchRequiredFailure(1));
        assertEquals(0, orderCount());
    }

    private int orderCount() {
        return jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Config {
        @Bean(destroyMethod = "shutdown")
        EmbeddedDatabase database() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        }

        @Bean
        JdbcTemplate jdbc(EmbeddedDatabase database) {
            return new JdbcTemplate(database);
        }

        @Bean
        JdbcTransactionManager transactionManager(EmbeddedDatabase database) {
            return new JdbcTransactionManager(database);
        }

        @Bean
        OrderWriter orderWriter(JdbcTemplate jdbc) {
            return new OrderWriter(jdbc);
        }

        @Bean
        Checkout checkout(OrderWriter writer, JdbcTemplate jdbc) {
            return new Checkout(writer, jdbc);
        }
    }

    static class OrderWriter {
        private final JdbcTemplate jdbc;

        OrderWriter(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        public void placeFromSameBean(int id) {
            this.insertThenFail(id);
        }

        @Transactional
        public void insertThenFail(int id) {
            jdbc.update("INSERT INTO orders(id) VALUES (?)", id);
            throw new PaymentRejected();
        }
    }

    static class Checkout {
        private final OrderWriter writer;
        private final JdbcTemplate jdbc;

        Checkout(OrderWriter writer, JdbcTemplate jdbc) {
            this.writer = writer;
            this.jdbc = jdbc;
        }

        public void place(int id) {
            writer.insertThenFail(id);
        }

        @Transactional
        public void catchRequiredFailure(int id) {
            try {
                writer.insertThenFail(id);
            } catch (PaymentRejected e) {
                jdbc.update("INSERT INTO orders(id) VALUES (?)", id + 1);
            }
        }
    }

    static class PaymentRejected extends RuntimeException {
    }
}
