package ru.learn.delivery;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

public final class WalletTestDatabase implements AutoCloseable {
    // Изолированная БД на тест. Режим PostgreSQL позволяет выполнять ON CONFLICT DO NOTHING.
    private final EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("wallet-" + java.util.UUID.randomUUID() + ";MODE=PostgreSQL")
            .addScript("classpath:delivery/wallet.sql")
            .build();

    private final JdbcTemplate jdbc = new JdbcTemplate(database);

    public WalletLedger ledger() {
        return new WalletLedger(database);
    }

    public void openWallet(String walletId) {
        jdbc.update("INSERT INTO lab_wallet(wallet_id, balance) VALUES (?, 0)", walletId);
    }

    public void applyWithoutInbox(String walletId, long amount) {
        jdbc.update("UPDATE lab_wallet SET balance = balance + ? WHERE wallet_id = ?", amount, walletId);
    }

    public long balance(String walletId) {
        return jdbc.queryForObject("SELECT balance FROM lab_wallet WHERE wallet_id = ?", Long.class, walletId);
    }

    @Override
    public void close() {
        database.shutdown();
    }
}
