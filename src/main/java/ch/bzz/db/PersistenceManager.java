package ch.bzz.db;

import ch.bzz.Config;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the one and only EntityManagerFactory of the application (DRY).
 * Creating a factory is expensive, therefore it is shared by all persistors.
 */
public final class PersistenceManager {

    private static final Logger log = LoggerFactory.getLogger(PersistenceManager.class);

    private static final String PERSISTENCE_UNIT = "localPU";

    private static EntityManagerFactory entityManagerFactory;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(PersistenceManager::close));
    }

    private PersistenceManager() {
        // utility class
    }

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            log.debug("Creating EntityManagerFactory for persistence unit {}", PERSISTENCE_UNIT);
            entityManagerFactory = Persistence.createEntityManagerFactory(
                    PERSISTENCE_UNIT, Config.getProperties());
        }
        return entityManagerFactory;
    }

    public static synchronized void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
            log.debug("EntityManagerFactory closed");
        }
    }
}
