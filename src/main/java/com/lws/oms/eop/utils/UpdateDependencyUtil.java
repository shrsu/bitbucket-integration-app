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
    boolean dependencyFound = false;
    int dependencyEndIndex = -1;
    int artifactIdIndex = -1;

    for (int i = 0; i < pomContent.size(); i++) {
      String line = pomContent.get(i);
      // Remove carriage returns and trim for parsing
      String trimmedLine = line.replace("\r", "").trim();

      if (trimmedLine.startsWith("<dependency>")) {
        insideDependencyBlock = true;
        isTargetDependency = false;
        dependencyEndIndex = -1;
        artifactIdIndex = -1;
      }

      // More flexible matching for artifactId - handle lines like <artifactId>name</artifactId>
      if (insideDependencyBlock && trimmedLine.startsWith("<artifactId>")) {
        String artifactId = trimmedLine
            .replace("<artifactId>", "")
            .replace("</artifactId>", "")
            .trim();
        if (artifactId.equals(dependency)) {
          isTargetDependency = true;
          dependencyFound = true;
          artifactIdIndex = i;
          log.info("Found target dependency: {}", dependency);
        }
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

      // Track the end of dependency block for inserting version if not present
      if (insideDependencyBlock && trimmedLine.startsWith("</dependency>")) {
        if (isTargetDependency && artifactIdIndex != -1 && !versionUpdated) {
          dependencyEndIndex = i;
          // Need to add version tag before </dependency>
          String artifactIdLine = pomContent.get(artifactIdIndex);
          String indentation = artifactIdLine.substring(0, artifactIdLine.indexOf("<artifactId>"));
          String versionLine = indentation + "    <version>" + newVersion + "</version>";
          pomContent.add(i, versionLine);
          versionUpdated = true;
          log.info("Added version {} for dependency {}", newVersion, dependency);
          break;
        }
        insideDependencyBlock = false;
        isTargetDependency = false;
      }
    }

    Map<String, Object> response = new HashMap<>();
    if (versionAlreadySame) {
      response.put("status", "failure");
      response.put("message", "Dependency already has the specified version. No update needed.");
    } else if (versionUpdated) {
      response.put("status", "success");
      response.put("message", "Dependency version updated successfully. Please verify it before committing.");
    } else if (!dependencyFound) {
      response.put("status", "failure");
      response.put("message", "Dependency not found in POM file. Please check the file manually.");
    } else {
      response.put("status", "failure");
      response.put("message", "Dependency found but could not update version.");
    }

    return response;
  }

}
