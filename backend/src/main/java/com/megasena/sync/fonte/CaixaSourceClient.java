package com.megasena.sync.fonte;

import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CaixaSourceClient {

    private static final Logger log = LoggerFactory.getLogger(CaixaSourceClient.class);

    private final RestClient restClient;

    public CaixaSourceClient(RestClient caixaRestClient) {
        this.restClient = caixaRestClient;
    }

    @Retry(name = "caixaApi")
    public CaixaConcursoResponse buscarUltimo() {
        log.debug("Buscando último concurso da fonte");
        return restClient.get()
                .uri("")
                .retrieve()
                .body(CaixaConcursoResponse.class);
    }

    @Retry(name = "caixaApi")
    public CaixaConcursoResponse buscarPorNumero(int numero) {
        log.debug("Buscando concurso {} da fonte", numero);
        return restClient.get()
                .uri("/{numero}", numero)
                .retrieve()
                .body(CaixaConcursoResponse.class);
    }
}
