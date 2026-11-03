package ch.bzz;

import ch.bzz.db.BookPersistor;
import ch.bzz.db.UserPersistor;
import ch.bzz.security.JwtHandler;
import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JavalinMainTest {
    private static TestDatabase database;
    private static Javalin app;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeAll
    static void startApp() throws Exception {
        database = new TestDatabase();
        app = JavalinMain.createApp().start(0);
    }

    @BeforeEach
    void resetData() throws Exception { database.reset(); }

    @AfterAll
    static void stopApp() throws Exception {
        if (app != null) app.stop();
        if (database != null) database.close();
    }

    private HttpResponse<String> request(String method, String path, String body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return CLIENT.send(builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        return request("POST", "/auth/login", JSON.writeValueAsString(Map.of(
                "email", email, "password", password)), null);
    }

    private String token() throws Exception {
        var response = login("max.mustermann@example.com", "geheim123");
        assertEquals(200, response.statusCode(), response.body());
        return JSON.readTree(response.body()).get("token").asText();
    }

    @Test
    void frontendAndAssetsAreServed() throws Exception {
        var page = request("GET", "/", "", null);
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Leseraum"));
        assertTrue(page.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertEquals(200, request("GET", "/app.js", "", null).statusCode());
        assertEquals(200, request("GET", "/style.css", "", null).statusCode());
    }

    @Test
    void testGetBooksWithLimit() throws Exception {
        var response = request("GET", "/books?limit=3", "", null);
        assertEquals(200, response.statusCode());
        assertEquals(JSON.valueToTree(new BookPersistor().getAll(3)), JSON.readTree(response.body()));
        assertEquals(3, JSON.readTree(response.body()).size());
    }

    @Test
    void testLoginReturnsJwt() throws Exception {
        var user = new UserPersistor().findByEmail("max.mustermann@example.com");
        assertEquals(user.getId(), JwtHandler.getUserId(token()));
    }

    @Test
    void invalidCredentialsHaveSameResponse() throws Exception {
        var wrong = login("max.mustermann@example.com", "wrong");
        var missing = login("nobody@example.com", "wrong");
        assertEquals(401, wrong.statusCode());
        assertEquals(401, missing.statusCode());
        assertEquals(wrong.body(), missing.body());
    }

    @Test
    void rejectsMalformedLoginBodies() throws Exception {
        for (String body : new String[]{"{", "{}", "{\"email\":null,\"password\":\"x\"}",
                "{\"email\":\"a\",\"password\":123}", "{\"email\":\"a\",\"password\":\" \"}"}) {
            assertEquals(400, request("POST", "/auth/login", body, null).statusCode(), body);
        }
    }

    @Test
    void changePasswordRequiresValidToken() throws Exception {
        String body = "{\"oldPassword\":\"geheim123\",\"newPassword\":\"new-secret\"}";
        for (String token : new String[]{null, "aaa.bbb.ccc", token() + "invalid"}) {
            assertEquals(401, request("PUT", "/auth/change-password", body, token).statusCode());
        }
    }

    @Test
    void rejectsInvalidNewPasswordWithoutChangingStoredPassword() throws Exception {
        String jwt = token();
        for (String value : new String[]{"null", "123", "[]", "\"\"", "\" \""}) {
            assertEquals(400, request("PUT", "/auth/change-password",
                    "{\"oldPassword\":\"geheim123\",\"newPassword\":" + value + "}", jwt).statusCode());
        }
        assertEquals(200, login("max.mustermann@example.com", "geheim123").statusCode());
    }

    @Test
    void changesPasswordAndSaltAndRejectsOldPassword() throws Exception {
        String jwt = token();
        String salt = new UserPersistor().findByEmail("max.mustermann@example.com").getPasswordSalt();
        assertEquals(401, request("PUT", "/auth/change-password",
                "{\"oldPassword\":\"wrong\",\"newPassword\":\"new-secret\"}", jwt).statusCode());
        assertEquals(200, request("PUT", "/auth/change-password",
                "{\"oldPassword\":\"geheim123\",\"newPassword\":\"new-secret\"}", jwt).statusCode());
        assertEquals(401, login("max.mustermann@example.com", "geheim123").statusCode());
        assertEquals(200, login("max.mustermann@example.com", "new-secret").statusCode());
        assertNotEquals(salt, new UserPersistor().findByEmail("max.mustermann@example.com").getPasswordSalt());
    }
}
