package com.megasena.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "megasena")
public class MegaSenaProperties {

    private Source source = new Source();
    private Sync sync = new Sync();
    private Admin admin = new Admin();

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public Sync getSync() { return sync; }
    public void setSync(Sync sync) { this.sync = sync; }
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public static class Source {
        private String baseUrl = "https://servicebus2.caixa.gov.br/portaldeloterias/api/megasena";
        private int connectTimeout = 5000;
        private int readTimeout = 10000;
        private String userAgent = "MegaSenaSync/1.0";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
        public int getReadTimeout() { return readTimeout; }
        public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    }

    public static class Sync {
        private String cron = "0 0 22 * * *";
        private int retryMaxAttempts = 3;
        private String retryWaitDuration = "2s";

        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
        public int getRetryMaxAttempts() { return retryMaxAttempts; }
        public void setRetryMaxAttempts(int retryMaxAttempts) { this.retryMaxAttempts = retryMaxAttempts; }
        public String getRetryWaitDuration() { return retryWaitDuration; }
        public void setRetryWaitDuration(String retryWaitDuration) { this.retryWaitDuration = retryWaitDuration; }
    }

    public static class Admin {
        private String token = "changeme";

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
