package com.megasena.sync.fonte;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.megasena.sync.config.HttpClientConfig;
import com.megasena.sync.config.MegaSenaProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaixaSourceClientWireMockTest {

    private WireMockServer wireMock;
    private CaixaSourceClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());

        MegaSenaProperties props = new MegaSenaProperties();
        props.getSource().setBaseUrl("http://localhost:" + wireMock.port());
        props.getSource().setConnectTimeout(2000);
        props.getSource().setReadTimeout(3000);

        RestClient restClient = new HttpClientConfig().caixaRestClient(props);
        client = new CaixaSourceClient(restClient);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void buscarUltimoSuccess() {
        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(validPayload(2700))));

        CaixaConcursoResponse result = client.buscarUltimo();
        assertNotNull(result);
        assertEquals(2700, result.getNumero());
        assertEquals("29/05/2024", result.getDataApuracao());
        assertEquals(6, result.getListaDezenas().size());
    }

    @Test
    void buscarPorNumeroSuccess() {
        wireMock.stubFor(get(urlEqualTo("/2700"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(validPayload(2700))));

        CaixaConcursoResponse result = client.buscarPorNumero(2700);
        assertNotNull(result);
        assertEquals(2700, result.getNumero());
    }

    @Test
    void buscarUltimoServerError() {
        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(HttpServerErrorException.class, () -> client.buscarUltimo());
    }

    @Test
    void buscarUltimoMalformedPayload() {
        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"invalid\": true}")));

        CaixaConcursoResponse result = client.buscarUltimo();
        assertNotNull(result);
        // Fields should be null/empty for invalid payload
    }

    private String validPayload(int numero) {
        return """
                {
                  "numero": %d,
                  "dataApuracao": "29/05/2024",
                  "listaDezenas": ["04", "17", "23", "38", "51", "60"],
                  "listaRateioPremio": [
                    { "descricaoFaixa": "Sena", "valorPremio": 52000000.00 }
                  ]
                }
                """.formatted(numero);
    }
}
