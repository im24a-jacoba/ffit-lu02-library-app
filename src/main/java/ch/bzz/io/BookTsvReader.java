package ch.bzz.io;

import ch.bzz.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads books out of a tab separated (TSV) UTF-8 file - and nothing else (SRP).
 * Tabs are used as delimiter because titles may contain commas and semicolons.
 */
public class BookTsvReader {

    private static final Logger log = LoggerFactory.getLogger(BookTsvReader.class);

    private static final String DELIMITER = "\t";
    private static final int COLUMN_COUNT = 5;
    private static final char BYTE_ORDER_MARK = '﻿';

    /**
     * @param filePath path of the TSV file
     * @return the books of the file, the header and invalid lines are skipped and logged
     * @throws IOException if the file does not exist or cannot be read
     */
    public List<Book> read(String filePath) throws IOException {
        List<Book> books = new ArrayList<>();
        Path path = Path.of(filePath);

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    line = stripByteOrderMark(line);
                }
                if (line.isBlank()) {
                    continue;
                }

                Book book = parseLine(line, lineNumber);
                if (book != null) {
                    books.add(book);
                }
            }
        }

        log.info("Read {} books from {}", books.size(), filePath);
        return books;
    }

    private Book parseLine(String line, int lineNumber) {
        String[] fields = line.split(DELIMITER, -1);

        if (fields.length < COLUMN_COUNT) {
            log.warn("Line {} has only {} columns and is skipped", lineNumber, fields.length);
            return null;
        }

        try {
            return new Book(
                    Integer.parseInt(fields[0].trim()),
                    fields[1].trim(),
                    fields[2].trim(),
                    fields[3].trim(),
                    Integer.parseInt(fields[4].trim())
            );
        } catch (NumberFormatException e) {
            if (lineNumber == 1) {
                log.debug("Line 1 is interpreted as header and is skipped");
            } else {
                log.warn("Line {} contains a non numeric id or publication year and is skipped", lineNumber);
            }
            return null;
        }
    }

    private String stripByteOrderMark(String line) {
        return !line.isEmpty() && line.charAt(0) == BYTE_ORDER_MARK ? line.substring(1) : line;
    }
}
