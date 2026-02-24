package com.lws.oms.eop.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateDependencyUtil {

  public static Map<String, Object> updateDependencyVersionInPom(List<String> pomContent, String dependency, String newVersion) {
    boolean insideDependencyBlock = false;
    boolean isTargetDependency = false;
    boolean versionUpdated = false;
    boolean versionAlreadySame = false;

    for (int i = 0; i < pomContent.size(); i++) {
      String line = pomContent.get(i);
      String trimmedLine = line.trim();

      if (trimmedLine.startsWith("<dependency>")) {
        insideDependencyBlock = true;
        isTargetDependency = false;
      }

      if (insideDependencyBlock && trimmedLine.startsWith("<artifactId>") && trimmedLine.contains(dependency)) {
        isTargetDependency = true;
        log.info("Found target dependency: {}", dependency);
      }

      if (insideDependencyBlock && isTargetDependency && trimmedLine.startsWith("<version>")) {
        String currentVersion = trimmedLine
            .replace("<version>", "")
            .replace("</version>", "")
            .trim();

        if (currentVersion.equals(newVersion)) {
          versionAlreadySame = true;
          log.warn("Dependency {} already has version {}.", dependency, newVersion);
          break;
        }

        String indentation = line.substring(0, line.indexOf("<version>"));
        pomContent.set(i, indentation + "<version>" + newVersion + "</version>");
        versionUpdated = true;
        log.info("Updated version for dependency {}: {}", dependency, newVersion);
        break; // Once updated, break
      }

      if (trimmedLine.startsWith("</dependency>")) {
        insideDependencyBlock = false;
      }
    }

    Map<String, Object> response = new HashMap<>();
    if (versionAlreadySame) {
      response.put("status", "failure");
      response.put("message", "Dependency already has the specified version. No update needed.");
    } else if (versionUpdated) {
      response.put("status", "success");
      response.put("message", "Dependency version updated successfully. Please verify it before committing.");
    } else {
      response.put("status", "failure");
      response.put("message", "Dependency not found in POM file. Please check the file manually.");
    }

    return response;
  }

}
