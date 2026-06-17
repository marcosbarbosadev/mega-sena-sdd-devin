package com.megasena.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "megasena.identidade")
public class IdentidadeProperties {

    private String projectId = "";
    private List<String> adminsBootstrap = List.of();

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public List<String> getAdminsBootstrap() { return adminsBootstrap; }
    public void setAdminsBootstrap(List<String> adminsBootstrap) { this.adminsBootstrap = adminsBootstrap; }
}
