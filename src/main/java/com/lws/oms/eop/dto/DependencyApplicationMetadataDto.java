package com.lws.oms.eop.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DependencyApplicationMetadataDto {

  private String dependencyName;
  private String artifactoryPath;
  private List<Map<String, Object>> versions;
  private List<ApplicationInfo> applications;


  @Data
  @AllArgsConstructor
  public static class ApplicationInfo {
    private String project;
    private String application;
    private String moduleName;
  }

}
