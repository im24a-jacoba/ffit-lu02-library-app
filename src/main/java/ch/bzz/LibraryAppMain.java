package ch.bzz;



import java.util.Scanner;



import java.util.Scanner;

public class LibraryAppMain {

    // Die beiden vorgegebenen Bücher als Konstanten
    private static final Book BOOK_1 = new Book(
            1,
            "978-3-8362-9544-4",
            "Java ist auch eine Insel",
            "Christian Ullenboom",
            2023
    );

    private static final Book BOOK_2 = new Book(
            2,
            "978-3-658-43573-8",
            "Grundkurs Java",
            "Dietmar Abts",
            2024
    );

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            switch (input) {
                case "listBooks" -> {
                    // Gibt mindestens den Titel pro Buch auf einer eigenen Zeile aus
                    System.out.println(BOOK_1.getTitle());
                    System.out.println(BOOK_2.getTitle());
                    // Alternativ mit vollständigen Infos:
                    // System.out.println(BOOK_1);
                    // System.out.println(BOOK_2);
                }
                case "help" -> {
                    System.out.println("Verfügbare Befehle: help, listBooks, quit");
                }
                case "quit" -> {
                    System.out.println("Programm wird beendet.");
                    running = false;
                }
                default -> {
                    System.out.println("Befehl '" + input + "' nicht erkannt.");
                }
            }
        }

        scanner.close();
    }
}

