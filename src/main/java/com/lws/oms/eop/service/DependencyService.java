package com.lws.oms.eop.service;

import com.lws.oms.eop.config.DependencyConfig;
import com.lws.oms.eop.config.DependencyConfig.Dependency;
import com.lws.oms.eop.config.DependencyConfig.Project;
import com.lws.oms.eop.dto.DependencyApplicationMetadataDto;
import com.lws.oms.eop.feign.MavenCentralFeignClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DependencyService {

  private final MavenCentralFeignClient mavenCentralFeignClient;
  private final List<Dependency> dependencies;

  public DependencyService(MavenCentralFeignClient mavenCentralFeignClient,
      DependencyConfig dependencyConfig) {
    this.mavenCentralFeignClient = mavenCentralFeignClient;
    this.dependencies = dependencyConfig.getDependencies();
  }

  public List<String> getAllDependencyNames() {
    return dependencies.stream()
        .map(Dependency::getName)
        .collect(Collectors.toList());
  }

  public DependencyApplicationMetadataDto getDependencyApplicationsWithMetadata(String dependencyName) {
    for (Dependency dependency : dependencies) {
      if (dependency.getName().equalsIgnoreCase(dependencyName)) {
        String query = String.format("g:\"%s\" AND a:\"%s\"",
            dependency.getGroupId(), dependency.getArtifactId());
        Map<String, Object> metadata = mavenCentralFeignClient.searchArtifact(query, 200, "gav");

        Map<String, Object> response = (Map<String, Object>) metadata.get("response");
        List<Map<String, Object>> docs =
            response != null ? (List<Map<String, Object>>) response.get("docs") : null;

        List<Map<String, Object>> versionsList = new ArrayList<>();
        if (docs != null) {
          for (Map<String, Object> doc : docs) {
            Object version = doc.get("v");
            Object timestamp = doc.get("timestamp");
            if (version != null && timestamp instanceof Number) {
              String versionStr = version.toString();
              versionsList.add(Map.of(
                  "version", version,
                  "name", versionStr,
                  "lastModified", ((Number) timestamp).longValue()
              ));
            }
          }

          versionsList.sort((a, b) -> {
            Long aModified = ((Number) a.get("lastModified")).longValue();
            Long bModified = ((Number) b.get("lastModified")).longValue();
            return Long.compare(bModified, aModified);
          });
        }

        List<DependencyApplicationMetadataDto.ApplicationInfo> allApplications = new ArrayList<>();
        if (dependency.getDependentApplications() != null &&
            dependency.getDependentApplications().getProjects() != null) {
          for (Project project : dependency.getDependentApplications().getProjects()) {
            if (project.getApplications() != null) {
              for (String app : project.getApplications()) {
                String applicationName = app;
                String moduleName = null;

                if (app.contains("/")) {
                  String[] parts = app.split("/", 2);
                  applicationName = parts[0];
                  moduleName = parts[1];
                }

                allApplications.add(
                    new DependencyApplicationMetadataDto.ApplicationInfo(
                        project.getName(),
                        applicationName,
                        moduleName
                    )
                );
              }
            }
          }
        }

        return new DependencyApplicationMetadataDto(
            dependency.getName(),
            String.format("%s:%s", dependency.getGroupId(), dependency.getArtifactId()),
            versionsList,
            allApplications
        );
      }
    }

    throw new IllegalArgumentException("Dependency not found: " + dependencyName);
  }

}
