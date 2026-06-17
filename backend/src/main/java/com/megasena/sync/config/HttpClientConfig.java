package com.megasena.sync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient caixaRestClient(MegaSenaProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getSource().getConnectTimeout());
        factory.setReadTimeout(properties.getSource().getReadTimeout());

        return RestClient.builder()
                .baseUrl(properties.getSource().getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("User-Agent", properties.getSource().getUserAgent())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
