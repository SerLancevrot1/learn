package ru.learn.interview;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaFetchLabTest {
    private static SessionFactory sessions;

    @BeforeAll
    static void seed() {
        sessions = new Configuration()
                .addAnnotatedClass(Purchase.class)
                .addAnnotatedClass(PurchaseLine.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:jpa-fetch-lab")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.generate_statistics", "true")
                .setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.query.fail_on_pagination_over_collection_fetch", "true")
                .buildSessionFactory();
        sessions.inTransaction(session -> {
            for (int id = 1; id <= 3; id++) {
                Purchase purchase = new Purchase(id);
                session.persist(purchase);
                session.persist(new PurchaseLine(id * 10, purchase));
                session.persist(new PurchaseLine(id * 10 + 1, purchase));
            }
        });
    }

    @AfterAll
    static void close() {
        if (sessions != null) {
            sessions.close();
        }
    }

    @BeforeEach
    void resetStatistics() {
        sessions.getStatistics().clear();
    }

    @Test
    void accessingUninitializedCollectionAfterSessionClosesFails() {
        Purchase detached = sessions.fromTransaction(session -> session.find(Purchase.class, 1));
        assertThrows(LazyInitializationException.class, () -> detached.lines.size());
        assertEquals(1, sessions.getStatistics().getPrepareStatementCount());
    }

    @Test
    void iteratingLazyCollectionsExecutesNPlusOneStatements() {
        int lineCount = sessions.fromTransaction(session -> session
                .createSelectionQuery("from Purchase p order by p.id", Purchase.class)
                .getResultList().stream().mapToInt(purchase -> purchase.lines.size()).sum());

        assertEquals(6, lineCount);
        assertEquals(4, sessions.getStatistics().getPrepareStatementCount());
    }

    @Test
    void fetchingCollectionInOneQueryInitializesItBeforeDetach() {
        List<Purchase> purchases = sessions.fromTransaction(session -> session
                .createSelectionQuery("from Purchase p left join fetch p.lines order by p.id", Purchase.class)
                .getResultList());

        assertEquals(3, purchases.size());
        assertEquals(6, purchases.stream().mapToInt(purchase -> purchase.lines.size()).sum());
        assertEquals(1, sessions.getStatistics().getPrepareStatementCount());
    }

    @Test
    void paginatingACollectionFetchIsRejected() {
        assertThrows(HibernateException.class, () -> sessions.inTransaction(session -> session
                .createSelectionQuery("from Purchase p left join fetch p.lines order by p.id", Purchase.class)
                .setMaxResults(2).getResultList()));
    }

    @Test
    void pageRootIdsFirstThenFetchTheirCollections() {
        List<Purchase> page = sessions.fromTransaction(session -> {
            List<Integer> ids = session.createSelectionQuery("select p.id from Purchase p order by p.id", Integer.class)
                    .setFirstResult(1).setMaxResults(2).getResultList();
            if (ids.isEmpty()) {
                return List.of();
            }
            Map<Integer, Purchase> byId = session.createSelectionQuery(
                            "from Purchase p left join fetch p.lines where p.id in :ids", Purchase.class)
                    .setParameterList("ids", ids).getResultList().stream()
                    .collect(Collectors.toMap(purchase -> purchase.id, Function.identity()));
            return ids.stream().map(byId::get).toList();
        });

        assertEquals(List.of(2, 3), page.stream().map(purchase -> purchase.id).toList());
        assertEquals(4, page.stream().mapToInt(purchase -> purchase.lines.size()).sum());
        assertEquals(2, sessions.getStatistics().getPrepareStatementCount());
    }

    @Entity(name = "Purchase")
    @Table(name = "lab_purchase")
    static class Purchase {
        @Id
        private int id;

        @OneToMany(mappedBy = "purchase", fetch = FetchType.LAZY)
        private List<PurchaseLine> lines = new ArrayList<>();

        protected Purchase() {
        }

        Purchase(int id) {
            this.id = id;
        }
    }

    @Entity(name = "PurchaseLine")
    @Table(name = "lab_purchase_line")
    static class PurchaseLine {
        @Id
        private int id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        private Purchase purchase;

        protected PurchaseLine() {
        }

        PurchaseLine(int id, Purchase purchase) {
            this.id = id;
            this.purchase = purchase;
        }
    }
}
