package ch.bzz.db;

import ch.bzz.model.Book;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Database access for {@link Book} objects - and nothing else (SRP).
 */
public class BookPersistor {

    private static final Logger log = LoggerFactory.getLogger(BookPersistor.class);

    /**
     * @return all books, ordered by id
     */
    public List<Book> getAll() {
        return getAll(0);
    }

    /**
     * @param limit maximum number of books, values &lt;= 0 mean no limit
     * @return the books, ordered by id
     */
    public List<Book> getAll(int limit) {
        try (EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager()) {
            var query = em.createQuery("SELECT b FROM Book b ORDER BY id", Book.class);
            if (limit > 0) {
                query.setMaxResults(limit);
            }
            return query.getResultList();
        } catch (RuntimeException e) {
            log.error("Error while reading the books from the database", e);
            return Collections.emptyList();
        }
    }

    /**
     * Saves all books. Entries with an already existing id are overwritten.
     */
    public void saveAll(List<Book> books) {
        try (EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager()) {
            try {
                em.getTransaction().begin();
                books.forEach(em::merge);
                em.getTransaction().commit();
                log.info("Saved {} books to the database", books.size());
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                log.error("Error during saving of books to the database", e);
            }
        }
    }
}
