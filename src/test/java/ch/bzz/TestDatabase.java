package ch.bzz;

import ch.bzz.db.PersistenceManager;
import ch.bzz.model.Book;
import ch.bzz.model.User;
import ch.bzz.security.PasswordHandler;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.UUID;

/** Each test class owns a fresh PostgreSQL schema, never the application's tables. */
final class TestDatabase implements AutoCloseable {
    private final String schema = "library_test_" + UUID.randomUUID().toString().replace("-", "");

    TestDatabase() throws Exception {
        PersistenceManager.close();
        execute("CREATE SCHEMA " + schema);
        System.setProperty("hibernate.default_schema", schema);
        System.setProperty("hibernate.hbm2ddl.auto", "update");
        PersistenceManager.getEntityManagerFactory();
    }

    void reset() throws Exception {
        try (var em = PersistenceManager.getEntityManagerFactory().createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM User").executeUpdate();
            em.createQuery("DELETE FROM Book").executeUpdate();
            em.persist(new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", 2018));
            em.persist(new Book(2, "978-0596009205", "Head First Java", "Kathy Sierra, Bert Bates", 2005));
            em.persist(new Book(3, "test-isbn-3", "Third test book", "Test Author", 2020));
            User user = new User(null, "Max", "Mustermann", LocalDate.of(1990, 5, 21),
                    "max.mustermann@example.com");
            byte[] salt = PasswordHandler.generateSalt();
            user.setPasswordSalt(PasswordHandler.encode(salt));
            user.setPasswordHash(PasswordHandler.encode(PasswordHandler.hashPassword("geheim123", salt)));
            em.persist(user);
            em.getTransaction().commit();
        }
    }

    private void execute(String sql) throws Exception {
        var properties = Config.getProperties();
        try (var connection = DriverManager.getConnection(
                properties.getProperty("jakarta.persistence.jdbc.url"),
                properties.getProperty("jakarta.persistence.jdbc.user"),
                properties.getProperty("jakarta.persistence.jdbc.password"));
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @Override
    public void close() throws Exception {
        PersistenceManager.close();
        execute("DROP SCHEMA " + schema + " CASCADE");
        System.clearProperty("hibernate.default_schema");
        System.clearProperty("hibernate.hbm2ddl.auto");
    }
}
