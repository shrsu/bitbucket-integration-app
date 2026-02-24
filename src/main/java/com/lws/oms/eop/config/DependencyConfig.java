package com.lws.oms.eop.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dependency-config")
public class DependencyConfig {

  private List<Dependency> dependencies;


  @Data
  public static class Dependency {
    private String name;
    private String groupId;
    private String artifactId;
    private DependentApplications dependentApplications;
  }


  @Data
  public static class DependentApplications {
    private List<Project> projects;
  }


  @Data
  public static class Project {
    private String name;
    private List<String> applications;
  }
}
