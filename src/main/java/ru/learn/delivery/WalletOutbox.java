package ru.learn.delivery;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Consumer;

/** Источник заявок на пополнение. Фактический баланс меняет потребитель в WalletLedger. */
public class WalletOutbox {
    private static final RowMapper<Credit> CREDIT = (row, index) ->
            new Credit(row.getString("event_id"), row.getString("wallet_id"), row.getLong("amount"));

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final TransactionTemplate relayTransaction;

    public WalletOutbox(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        var manager = new JdbcTransactionManager(dataSource);
        transaction = new TransactionTemplate(manager);
        relayTransaction = new TransactionTemplate(manager);
        relayTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Повтор с тем же payload безопасен. Входной eventId генерируется один раз на заявку. */
    public void enqueue(Credit credit) {
        transaction.executeWithoutResult(status -> {
            int inserted = jdbc.update("""
                    INSERT INTO lab_credit_request(event_id, wallet_id, amount) VALUES (?, ?, ?)
                    ON CONFLICT DO NOTHING
                    """, credit.eventId(), credit.walletId(), credit.amount());
            if (inserted == 0) {
                Credit existing = jdbc.queryForObject("""
                        SELECT event_id, wallet_id, amount FROM lab_credit_request WHERE event_id = ?
                        """, CREDIT, credit.eventId());
                if (!credit.equals(existing)) {
                    throw new IllegalArgumentException("Event id is already used for a different credit");
                }
                return;
            }
            jdbc.update("INSERT INTO lab_credit_outbox(event_id) VALUES (?)", credit.eventId());
        });
    }

    /** Publisher возвращается после подтверждения брокера и ограничивает время ожидания. */
    public boolean publishNext(Consumer<Credit> publisher) {
        return Boolean.TRUE.equals(relayTransaction.execute(status -> {
            List<String> pending = jdbc.queryForList("""
                    SELECT event_id FROM lab_credit_outbox WHERE published = FALSE
                    ORDER BY position LIMIT 1 FOR UPDATE SKIP LOCKED
                    """, String.class);
            if (pending.isEmpty()) {
                return false;
            }
            String eventId = pending.get(0);
            Credit credit = jdbc.queryForObject("""
                    SELECT event_id, wallet_id, amount FROM lab_credit_request WHERE event_id = ?
                    """, CREDIT, eventId);
            publisher.accept(credit);
            jdbc.update("UPDATE lab_credit_outbox SET published = TRUE WHERE event_id = ?", eventId);
            return true;
        }));
    }

    public record Credit(String eventId, String walletId, long amount) {
        public Credit {
            if (eventId == null || eventId.isBlank() || walletId == null || walletId.isBlank() || amount <= 0) {
                throw new IllegalArgumentException("A stable event id, a wallet id and a positive amount are required");
            }
        }
    }
}
