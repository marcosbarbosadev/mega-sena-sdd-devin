package com.megasena.sync.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractWireMockIntegrationTest {

    protected static final WireMockServer wireMock;
    private static final MySQLContainer<?> mysql;

    @LocalServerPort
    protected int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    static {
        mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("megasena")
                .withUsername("test")
                .withPassword("test");
        mysql.start();

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
    void resetState() {
        wireMock.resetAll();
        WireMock.configureFor("localhost", wireMock.port());
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE concurso_dezena");
        jdbcTemplate.execute("TRUNCATE TABLE sync_run");
        jdbcTemplate.execute("TRUNCATE TABLE concurso");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
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
