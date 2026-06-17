package com.megasena.sync.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractWireMockIntegrationTest {

    protected static WireMockServer wireMock;

    @LocalServerPort
    protected int port;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("megasena")
            .withUsername("test")
            .withPassword("test");

    static {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("megasena.admin.token", () -> "test-admin-token");
        registry.add("megasena.source.base-url", () -> "http://localhost:" + wireMock.port());
        registry.add("megasena.sync.cron", () -> "-");
        registry.add("resilience4j.retry.instances.caixaApi.max-attempts", () -> "1");
        registry.add("resilience4j.retry.instances.caixaApi.wait-duration", () -> "100ms");
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
        WireMock.configureFor("localhost", wireMock.port());
    }

    @AfterEach
    void verifyWireMock() {
        wireMock.resetAll();
    }

    protected static String concursoPayload(int numero, String data, String[] dezenas, double premio) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"numero\":").append(numero).append(",");
        sb.append("\"dataApuracao\":\"").append(data).append("\",");
        sb.append("\"listaDezenas\":[");
        for (int i = 0; i < dezenas.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(dezenas[i]).append("\"");
        }
        sb.append("],");
        sb.append("\"listaRateioPremio\":[{\"descricaoFaixa\":\"Sena\",\"valorPremio\":").append(premio).append("}]");
        sb.append("}");
        return sb.toString();
    }
}
