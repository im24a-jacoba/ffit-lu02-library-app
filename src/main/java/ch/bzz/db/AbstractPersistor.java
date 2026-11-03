package ch.bzz.db;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for all database access. The generic type T says which entity a
 * concrete persistor works with, so {@link #save(Object)} and {@link #getAll(int)}
 * are type safe without every subclass repeating the transaction handling (DRY).
 *
 * @param <T> the entity this persistor reads and writes
 */
public abstract class AbstractPersistor<T> implements AutoCloseable {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Class<T> entityClass;

    protected AbstractPersistor(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * Saves one entity. An entry with an already existing id is overwritten.
     */
    public void save(T entity) {
        executeTransaction(em -> em.merge(entity));
    }

    /**
     * Saves all entities in one transaction. Entries with an already existing id are overwritten.
     */
    public void saveAll(List<T> entities) {
        executeTransaction(em -> entities.forEach(em::merge));
        log.info("Saved {} entities of type {}", entities.size(), entityClass.getSimpleName());
    }

    /**
     * @return all entities, ordered by id
     */
    public List<T> getAll() {
        return getAll(0);
    }

    /**
     * @param limit maximum number of entities, values &lt;= 0 mean no limit
     * @return the entities, ordered by id
     */
    public List<T> getAll(int limit) {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e ORDER BY id";

        return query(em -> {
            var typedQuery = em.createQuery(jpql, entityClass);
            if (limit > 0) {
                typedQuery.setMaxResults(limit);
            }
            return typedQuery.getResultList();
        });
    }

    /**
     * @return the entity with this id, or null if there is none
     */
    public T findById(Object id) {
        return query(em -> em.find(entityClass, id));
    }

    /**
     * Runs a read-only action and returns its result.
     *
     * Database failures propagate so callers do not mistake outages for empty results.
     */
    protected <R> R query(Function<EntityManager, R> action) {
        try (EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager()) {
            return action.apply(em);
        } catch (RuntimeException e) {
            log.error("Error while reading {} from the database", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Runs an action inside a transaction and rolls it back if it fails.
     * The exception is rethrown so the caller can decide how to react.
     */
    protected void executeTransaction(Consumer<EntityManager> action) {
        try (EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager()) {
            try {
                em.getTransaction().begin();
                action.accept(em);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                log.error("Error during transaction:", e);
                throw e;
            }
        }
    }

    /**
     * Each operation already closes its EntityManager. The shared factory belongs
     * to PersistenceManager and must not be closed while other persistors use it.
     */
    @Override
    public void close() {
        // No per-instance resources; the application owns the shared factory.
    }
}
