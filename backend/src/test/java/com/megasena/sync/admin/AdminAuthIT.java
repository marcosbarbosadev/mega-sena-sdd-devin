package com.megasena.sync.admin;

import com.megasena.sync.support.AbstractWireMockIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminAuthIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void statusEndpointRequiresAuth() {
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/admin/sync/status", String.class);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void statusEndpointRejectsWrongToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("wrong-token");

        var response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/sync/status",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void runEndpointRequiresAuth() {
        HttpHeaders headers = new HttpHeaders();
        var response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/sync/run",
                HttpMethod.POST, new HttpEntity<>("", headers), String.class);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void concursosEndpointIsOpen() {
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/concursos/99999", String.class);
        // 404 is fine — point is that it's not 401
        assertEquals(404, response.getStatusCode().value());
    }
}
