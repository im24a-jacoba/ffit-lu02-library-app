package ch.bzz.db;

import ch.bzz.model.Book;

/**
 * Database access for {@link Book} objects - and nothing else (SRP).
 * Reading, writing and the transaction handling come from {@link AbstractPersistor}.
 */
public class BookPersistor extends AbstractPersistor<Book> {

    public BookPersistor() {
        super(Book.class);
    }
}
