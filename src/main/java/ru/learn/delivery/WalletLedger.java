package ru.learn.delivery;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/** Учебный кошелёк. Схема PostgreSQL/H2 в ресурсах тестов. Суммы хранятся в копейках. */
public class WalletLedger {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public WalletLedger(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        // Обработчик подтверждает сообщение после возврата из applyOnce().
        // Даже при наличии внешней транзакции метод должен завершить собственный commit.
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean applyOnce(String eventId, String walletId, long amount) {
        if (eventId == null || eventId.isBlank() || amount <= 0) {
            throw new IllegalArgumentException("A stable event id and a positive amount are required");
        }

        return Boolean.TRUE.equals(transaction.execute(status -> {
            // Уникальный индекс решает гонку двух consumer-ов на уровне БД.
            // Проверка SELECT, за которой следует обычный INSERT, оставила бы окно для гонки.
            int inserted = jdbc.update("""
                    INSERT INTO lab_inbox(event_id) VALUES (?)
                    ON CONFLICT DO NOTHING
                    """, eventId);
            if (inserted == 0) {
                return false;
            }

            // Маркер и изменение баланса фиксируются вместе. Если кошелька нет, rollback
            // удалит и маркер: после исправления причины повтор сможет обработать событие.
            credit(walletId, amount);
            return true;
        }));
    }

    private void credit(String walletId, long amount) {
        int updated = jdbc.update("UPDATE lab_wallet SET balance = balance + ? WHERE wallet_id = ?", amount, walletId);
        if (updated != 1) {
            throw new IllegalStateException("Wallet does not exist: " + walletId);
        }
    }
}
