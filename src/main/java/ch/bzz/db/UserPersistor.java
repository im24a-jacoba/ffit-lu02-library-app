package ch.bzz.db;

import ch.bzz.model.User;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Database access for {@link User} objects - and nothing else (SRP).
 */
public class UserPersistor {

    private static final Logger log = LoggerFactory.getLogger(UserPersistor.class);

    /**
     * Saves a user. An entry with an already existing id is overwritten.
     *
     * @return true if the user could be stored
     */
    public boolean save(User user) {
        try (EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager()) {
            try {
                em.getTransaction().begin();
                em.merge(user);
                em.getTransaction().commit();
                log.info("Saved user {}", user.getEmail());
                return true;
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                log.error("Error during saving of the user {} to the database", user.getEmail(), e);
                return false;
            }
        }
    }

    /**
     * @return all users, ordered by id
     */
    public List<User> getAll() {
        try (EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager()) {
            return em.createQuery("SELECT u FROM User u ORDER BY id", User.class).getResultList();
        } catch (RuntimeException e) {
            log.error("Error while reading the users from the database", e);
            return Collections.emptyList();
        }
    }
}
