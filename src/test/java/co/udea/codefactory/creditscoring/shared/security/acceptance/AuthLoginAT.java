package co.udea.codefactory.creditscoring.shared.security.acceptance;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Task 7.9 — AuthLoginAT (Acceptance Test)
 * POST /api/v1/auth/login with valid credentials → 200 + { token, role, expiresAt }
 * POST /api/v1/auth/login with missing credentials → 400 or 401
 *
 * Note: V29 migration sets admin password hash for 'admin123'.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthLoginAT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void login_withValidCredentials_returnsTokenAndRole() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"admin\", \"password\": \"admin123\"}")
        .when()
                .post("/api/v1/auth/login")
        .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("role", notNullValue())
                .body("expiresAt", notNullValue());
    }

    @Test
    void login_withMissingCredentials_returnsClientError() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"\", \"password\": \"\"}")
        .when()
                .post("/api/v1/auth/login")
        .then()
                .statusCode(oneOf(400, 401));
    }

    @Test
    void login_withWrongPassword_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"admin\", \"password\": \"wrong-password\"}")
        .when()
                .post("/api/v1/auth/login")
        .then()
                .statusCode(401);
    }
}
