package ch.bzz.db;

import ch.bzz.model.User;

/**
 * Database access for {@link User} objects - and nothing else (SRP).
 * Only the lookup by email is specific to users, everything else is inherited.
 */
public class UserPersistor extends AbstractPersistor<User> {

    public UserPersistor() {
        super(User.class);
    }

    /**
     * @return the user with this email address, or null if there is none
     */
    public User findByEmail(String email) {
        return query(em -> em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Saves a user without letting a failure (e.g. a duplicate email) reach the caller.
     *
     * @return true if the user could be stored
     */
    public boolean trySave(User user) {
        try {
            save(user);
            log.info("Saved user {}", user.getEmail());
            return true;
        } catch (RuntimeException e) {
            // already logged in executeTransaction, the caller only needs the result
            return false;
        }
    }
}
