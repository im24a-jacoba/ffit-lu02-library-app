# Library App – LU02 bis LU06

Java-Lernprojekt mit PostgreSQL/Hibernate, Konsole und Javalin REST-API.
Die gelieferten Aufträge werden durch folgende Funktionen abgedeckt:

- LU02: erweiterbare Konsolenbefehle, Bücher aus PostgreSQL, TSV-Import.
- LU03: getrennte Verantwortlichkeiten, gemeinsame Datenbanklogik, SLF4J/Logback,
  JPA-Entities und automatische Tabellenanlage.
- LU04: Bücher-API und Benutzer mit gesalzenem Passwort-Hash.
- LU05: Interfaces, Vererbung und generischer `AbstractPersistor<T>`.
- LU06: JWT-Login und authentifizierter Passwortwechsel.

Zusätzlich ist ein einfaches Frontend enthalten: Bücher durchsuchen, blättern,
anmelden, abmelden und Passwort ändern. Es läuft direkt mit der API.

## 1. Voraussetzungen

- PostgreSQL läuft, Datenbank und Benutzer existieren.
- JDK 22 oder 23. Der Gradle-Wrapper 8.13 funktioniert hier mit corretto-23;
  JDK 25 wird von diesem Wrapper nicht unterstützt.
- IntelliJ: Settings → Build Tools → Gradle → Gradle JVM → corretto-23.
- Alle Befehle aus dem Projektverzeichnis ausführen.

PowerShell bei Bedarf auf das vorhandene JDK setzen:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\corretto-23.0.2"
```

## 2. Konfiguration

Falls `config.properties` noch nicht existiert, `config.properties.template` kopieren.
Eine bestehende Datei nicht überschreiben. Beispiel für die bisherige lokale Einrichtung:

```properties
jakarta.persistence.jdbc.url=jdbc:postgresql://localhost:5433/localdb
jakarta.persistence.jdbc.user=localuser
jakarta.persistence.jdbc.password=DEIN_DB_PASSWORT
jwt.secret=DEIN_ZUFAELLIGES_SECRET_MIT_MINDESTENS_32_ZEICHEN
```

Port, Datenbank, Benutzer und Passwort an deine Installation anpassen.
Hibernate erstellt die Tabellen beim ersten Datenbankzugriff. Die Datenbank selbst
muss vorher existieren. Ein zufälliges Secret kann PowerShell erzeugen:

```powershell
$bytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Ergebnis als `jwt.secret` eintragen. Die lokale Config ist von Git ausgeschlossen.
Ohne ausreichend langes Secret wird ein temporärer Schlüssel verwendet; dann
werden Tokens nach einem Neustart ungültig.

## 3. Konsole und Daten

```powershell
.\gradlew.bat runConsole --console=plain -q
```

Beispielbefehle innerhalb der Konsole:

```text
help
importBooks data/books.tsv
listBooks 10
createUser Max Mustermann 1990-05-21 max.mustermann@example.com geheim123
quit
```

`createUser` für dieselbe E-Mail nur einmal ausführen. Die Anmeldung benötigt
einen selbst angelegten Benutzer; Tests legen keine Benutzer in deiner normalen
Datenbank an. Namen und Passwort sind einzelne Argumente ohne Leerzeichen.
`importBooks` akzeptiert auch einen unquoted Dateipfad mit Leerzeichen.
TSV-Spalten: id, isbn, title, author, year; erste Zeile darf eine Kopfzeile sein.
Die mitgelieferte Datei enthält 8841 Bücher. Gleiche IDs werden überschrieben,
der wiederholte Import legt keine Duplikate an. Ungültige Zeilen werden protokolliert.
`listBooks` ohne Limit oder mit Limit <= 0 zeigt alle Bücher.

## 4. API starten

```powershell
.\gradlew.bat runApi
```

Frontend im Browser: http://localhost:7070/
Bücher als JSON: http://localhost:7070/books?limit=10
Stoppen mit Strg+C. Alternativ startet `start.ps1` die App mit dem vorhandenen JDK 23.
Die Anmeldung bleibt nur im Arbeitsspeicher des Tabs; Neuladen meldet dich ab.

- `GET /books?limit=10`: öffentliche Bücherliste als JSON.
- `POST /auth/login`: JSON `{"email":"max.mustermann@example.com","password":"geheim123"}`.
  Antwort `{"token":"..."}`; falsche Zugangsdaten liefern 401.
- `PUT /auth/change-password`: Header `Authorization: Bearer <token>` und JSON
  `{"oldPassword":"geheim123","newPassword":"neuesPasswort456"}`.
  Erfolg 200, ungültiger Token oder falsches altes Passwort 401, ungültiger Body 400.

Für beide Schreibanfragen `Content-Type: application/json` verwenden.
JWTs gelten eine Stunde. Signatur und Ablauf werden geprüft. Ein Passwortwechsel
ändert Hash und Salt. Bereits ausgestellte Tokens gelten bis zu ihrem Ablauf weiter;
der Passwortwechsel verlangt zusätzlich das aktuelle Passwort.
Passwörter werden entsprechend der Unterrichtsvorgabe als SHA-256 mit Salt gespeichert.
Das Projekt ist eine Lernanwendung, kein fertig abgesicherter öffentlicher Produktivdienst.

## 5. Postman von Hand

1. `postman/LibraryApp.postman_collection.json` importieren.
2. Collection-Variablen `baseUrl`, `email`, `password`, `newPassword` anpassen.
3. API starten und Requests in der angegebenen Reihenfolge ausführen.
4. Login speichert den Token automatisch in der Collection-Variable `token`.
5. Die letzte Anfrage setzt das Passwort wieder auf den ursprünglichen Wert zurück.

Die Collection ändert das Passwort des ausgewählten Benutzers. Mit einem Testbenutzer
arbeiten und alle Schritte bis zum Zurücksetzen ausführen.
Für die JWT-Unterrichtsaufgabe einen Token eines lokalen Testbenutzers auf jwt.io
analysieren: Header, `sub`, `userId`, `iat` und `exp`. Den Signaturschlüssel nicht hochladen.

## 6. Automatisierte Tests

```powershell
.\gradlew.bat clean test
```

PostgreSQL und eine gültige `config.properties` werden benötigt. Der DB-Benutzer muss
Schemas erstellen dürfen. Jede Integrationstestklasse erstellt ein zufälliges
`library_test_<UUID>`-Schema und entfernt es nach dem Test. Die normalen Tabellen
werden weder geleert noch geändert. Die Tests erstellen ihre eigenen Bücher und
Benutzer vor jedem Test; Reihenfolge und bestehende Daten spielen keine Rolle.
Die API-Tests verwenden einen freien Port und stoppen ihren Server danach.
Bei hart abgebrochenen Testprozessen kann ein temporäres Testschema übrig bleiben.

Ergebnisbericht: `build/reports/tests/test/index.html`.
Tests decken Konsole, Import inklusive Wiederholung, Login, JSON-Validierung,
Passwortwechsel, Salt-Wechsel, ungültige Signaturen und abgelaufene Tokens ab.
`useJUnitPlatform()` in `build.gradle` muss aktiviert bleiben.

GitHub Actions startet PostgreSQL, erzeugt eine Test-Config, führt die Tests aus und
stellt den HTML-Bericht als Artefakt bereit. Nach dem Push den tatsächlichen Lauf
unter Actions prüfen; ein lokaler Erfolg bestätigt noch keinen GitHub-Lauf.

## Fehlersuche

- Verbindung verweigert: PostgreSQL-Dienst und den Port aus der Config prüfen.
- Login 401: Benutzer über Konsole anlegen bzw. aktuelles Passwort benutzen.
- Port 7070 belegt: bereits gestartete API beenden.
- Gradle-Fehler mit JDK 25: Gradle JVM/JAVA_HOME auf JDK 22 oder 23 setzen.
- Weitere Details stehen in `logs/app.log`; Logs werden täglich rotiert.
