package com.lws.oms.eop.service;

import static com.lws.oms.eop.utils.ErrorUtils.extractMeaningfulErrorMessage;

import com.lws.oms.eop.exception.CustomApiException;
import com.lws.oms.eop.model.CommitInfo;
import com.lws.oms.eop.model.FileInfo;
import com.lws.oms.eop.model.PrBuildRequestItem;
import com.lws.oms.eop.model.PrInfo;
import com.lws.oms.eop.model.RepositoryInfo;
import com.lws.oms.eop.model.requests.CommitRequest;
import com.lws.oms.eop.model.requests.UpdateDependencyVersionRequest;
import com.lws.oms.eop.utils.UpdateDependencyUtil;
import feign.FeignException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BitBucketIntegrationService {

  private final BitbucketApiService bitbucketApiService;

  @Async("taskExecutor")
  public CompletableFuture<Map<String, Object>> fetchBranchDataAsync(
      RepositoryInfo repositoryInfo,
      String authHeader) {

    Map<String, Object> branchResponse = new HashMap<>();
    branchResponse.put("projectName", repositoryInfo.getProjectName());
    branchResponse.put("repository", repositoryInfo.getRepoSlug());

    try {
      Map<String, Object> branchData = bitbucketApiService.retrieveBranches(repositoryInfo, authHeader);
      branchResponse.put("status", "success");

      List<Map<String, Object>> branches = (List<Map<String, Object>>) branchData.get("values");
      List<Map<String, Object>> formattedBranches = new ArrayList<>();

      if (branches != null) {
        for (Map<String, Object> branch : branches) {
          Map<String, Object> formattedBranch = new HashMap<>();
          formattedBranch.put("name", branch.get("name"));

          Map<String, Object> target = (Map<String, Object>) branch.get("target");
          if (target != null) {
            formattedBranch.put("latestCommit", target.get("hash"));
          }

          formattedBranches.add(formattedBranch);
        }
      }

      branchResponse.put("branches", formattedBranches);
    } catch (FeignException e) {
      branchResponse.put("status", "error");
      branchResponse.put("error", extractMeaningfulErrorMessage(e));
    } catch (Exception e) {
      branchResponse.put("status", "error");
      branchResponse.put("error", "Error fetching branches: Server Error");
    }

    return CompletableFuture.completedFuture(branchResponse);
  }

  @Async("taskExecutor")
  public CompletableFuture<Map<String, Object>> createBranchAsync(
      RepositoryInfo repositoryInfo,
      String branchName,
      String startPoint,
      String authHeader) {

    Map<String, Object> branchResponse = new HashMap<>();
    branchResponse.put("projectName", repositoryInfo.getProjectName());
    branchResponse.put("repo", repositoryInfo.getRepoSlug());

    try {
      Map<String, Object> newBranch = bitbucketApiService.createBranch(
          repositoryInfo, branchName, startPoint, authHeader);
      branchResponse.put("status", "success");
      branchResponse.put("newBranch", newBranch);
      branchResponse.put("message", String.format("Successfully created branch '%s' from '%s'", branchName, startPoint));
    } catch (FeignException e) {
      String errorMsg = extractMeaningfulErrorMessage(e);
      branchResponse.put("status", "error");
      branchResponse.put("error", errorMsg);
      branchResponse.put("message", String.format("Failed to create branch '%s' from '%s': %s", branchName, startPoint, errorMsg));
    } catch (Exception e) {
      branchResponse.put("status", "error");
      branchResponse.put("error", "Error creating branches: Server Error");
      branchResponse.put("message", String.format("Failed to create branch '%s' from '%s': Server Error", branchName, startPoint));
    }

    return CompletableFuture.completedFuture(branchResponse);
  }

  @Async("taskExecutor")
  public CompletableFuture<Map<String, Object>> createPullRequestAsync(
      PrInfo prInfo,
      RepositoryInfo repositoryInfo,
      String authHeader) {

    Map<String, Object> prResponse = new HashMap<>();
    prResponse.put("projectName", repositoryInfo.getProjectName());
    prResponse.put("repo", repositoryInfo.getRepoSlug());

    String source = prInfo.getFromBranch();
    String target = prInfo.getToBranch();

    try {
      Map<String, Object> result = bitbucketApiService.createPullRequest(prInfo, repositoryInfo, authHeader);
      prResponse.put("status", "success");
      prResponse.put("result", result);
      prResponse.put("message", String.format("Successfully created pull request from '%s' to '%s'", source, target));
    } catch (FeignException e) {
      String errorMsg = extractMeaningfulErrorMessage(e);
      prResponse.put("status", "error");
      prResponse.put("error", errorMsg);
      prResponse.put("message", String.format("Failed to create pull request from '%s' to '%s': %s", source, target, errorMsg));
    } catch (Exception e) {
      prResponse.put("status", "error");
      prResponse.put("error", "Error creating PR: Internal Server Error");
      prResponse.put("message", String.format("Failed to create pull request from '%s' to '%s': Internal Server Error", source, target));
    }

    return CompletableFuture.completedFuture(prResponse);
  }

  @Async("taskExecutor")
  public CompletableFuture<Map<String, Object>> getFileContentAsync(
      FileInfo fileInfo,
      String branchName,
      String authHeader) {

    Map<String, Object> fileResponse = new HashMap<>();
    fileResponse.put("projectName", fileInfo.getRepositoryInfo().getProjectName());
    fileResponse.put("repo", fileInfo.getRepositoryInfo().getRepoSlug());
    fileResponse.put("file", fileInfo.getFileName());
    fileResponse.put("module", fileInfo.getRepositoryInfo().getModuleName());
    fileResponse.put("searchPath", fileInfo.getSearchPath());

    try {
      String latestCommit = bitbucketApiService.getLatestCommit(
          fileInfo.getRepositoryInfo(),
          branchName,
          authHeader
      );

      String filePath = bitbucketApiService.findFilePathByName(
          fileInfo.getRepositoryInfo(),
          latestCommit,
          fileInfo.getFileName(),
          authHeader,
          Optional.ofNullable(fileInfo.getRepositoryInfo().getModuleName()).orElse(""),
          Optional.ofNullable(fileInfo.getSearchPath()).orElse("")
      );

      List<String> content = bitbucketApiService.getFileContent(
          fileInfo.getRepositoryInfo(),
          latestCommit,
          filePath,
          authHeader
      );

      fileResponse.put("status", "success");
      fileResponse.put("content", content);
    } catch (FeignException e) {
      fileResponse.put("status", "error");
      fileResponse.put("error", extractMeaningfulErrorMessage(e));
    } catch (CustomApiException e) {
      fileResponse.put("status", "error");
      fileResponse.put("error", e.getMessage());
    } catch (Exception e) {
      fileResponse.put("status", "error");
      fileResponse.put("error", "Error fetching file content: Internal Server Error");
    }

    return CompletableFuture.completedFuture(fileResponse);
  }

  @Async("taskExecutor")
  public CompletableFuture<Map<String, Object>> createCommitAsync(
      CommitInfo commitInfo,
      CommitRequest commitRequest,
      String authHeader) {

    Map<String, Object> commitResponse = new HashMap<>();
    RepositoryInfo repositoryInfo = commitInfo.getRepositoryInfo();

    commitResponse.put("projectName", repositoryInfo.getProjectName());
    commitResponse.put("repo", repositoryInfo.getRepoSlug());
    commitResponse.put("file", commitInfo.getFileName());
    commitResponse.put("module", repositoryInfo.getModuleName());
    commitResponse.put("searchPath", commitInfo.getSearchPath());

    try {
      String latestCommit = bitbucketApiService.getLatestCommit(
          repositoryInfo,
          commitRequest.getBranch(),
          authHeader
      );

      String filePath = bitbucketApiService.findFilePathByName(
          repositoryInfo,
          latestCommit,
          commitInfo.getFileName(),
          authHeader,
          Optional.ofNullable(repositoryInfo.getModuleName()).orElse(""),
          Optional.ofNullable(commitInfo.getSearchPath()).orElse("")
      );

      Map<String, Object> commitResult = bitbucketApiService.createCommit(
          repositoryInfo,
          filePath,
          commitInfo.getContent(),
          commitInfo.getCommitMessage(),
          commitRequest.getBranch(),
          authHeader,
          latestCommit
      );

      commitResponse.put("status", "success");
      commitResponse.put("commitResult", commitResult);
    } catch (FeignException e) {
      commitResponse.put("status", "error");
      commitResponse.put("error", "Error committing file: " + extractMeaningfulErrorMessage(e));
    } catch (CustomApiException e) {
      commitResponse.put("status", "error");
      commitResponse.put("error", "Error committing file content: " + e.getMessage());
    } catch (Exception e) {
      commitResponse.put("status", "error");
      commitResponse.put("error", "Error committing file content: Internal Server Error");
    }

    return CompletableFuture.completedFuture(commitResponse);
  }

  @Async("taskExecutor")
  public CompletableFuture<Map<String, Object>> updateDependencyVersionAsync(
      RepositoryInfo repoInfo,
      UpdateDependencyVersionRequest request,
      String authHeader) {

    Map<String, Object> updateResponse = new HashMap<>();
    updateResponse.put("projectName", repoInfo.getProjectName());
    updateResponse.put("repo", repoInfo.getRepoSlug());
    updateResponse.put("module", repoInfo.getModuleName());

    try {
      String latestCommit = bitbucketApiService.getLatestCommit(
          repoInfo,
          request.getBranchName(),
          authHeader
      );

      String pomPath = bitbucketApiService.findFilePathByName(
          repoInfo,
          latestCommit,
          "pom.xml",
          authHeader,
          repoInfo.getModuleName(),
          ""
      );

      // Preserve original EOL style when reading pom.xml
      var pomWithEol = bitbucketApiService.getFileContentWithEol(
          repoInfo,
          latestCommit,
          pomPath,
          authHeader
      );

      List<String> pomContent = pomWithEol.getLines();

      Map<String, Object> updateResult = UpdateDependencyUtil.updateDependencyVersionInPom(
          pomContent,
          request.getDependency(),
          request.getVersion()
      );

      updateResponse.put("status", updateResult.get("status"));
      updateResponse.put("message", updateResult.get("message"));
      updateResponse.put("eol", pomWithEol.getEol());
      updateResponse.put("pomContent", pomContent);
    } catch (FeignException e) {
      updateResponse.put("status", "error");
      updateResponse.put("error", "Error updating dependency version: " + extractMeaningfulErrorMessage(e));
    } catch (CustomApiException e) {
      updateResponse.put("status", "error");
      updateResponse.put("error", "Error updating dependency version: " + e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error updating dependency version for repo {}: {}", repoInfo.getRepoSlug(), e.getMessage(), e);
      updateResponse.put("status", "error");
      updateResponse.put("error", "Error updating dependency version: " + e.getMessage());
    }

    return CompletableFuture.completedFuture(updateResponse);
  }

  @Async("taskExecutor")
  public CompletableFuture<Map<String, Object>> getCommitBuildStatusesAsync(
      RepositoryInfo repositoryInfo,
      String commitHash,
      String authHeader) {

    Map<String, Object> result = new HashMap<>();
    result.put("projectName", repositoryInfo.getProjectName());
    result.put("repoSlug", repositoryInfo.getRepoSlug());
    result.put("commitHash", commitHash);

    try {
      Map<String, Object> buildStatuses = bitbucketApiService.getCommitBuildStatuses(
          repositoryInfo,
          commitHash,
          authHeader
      );
      result.put("buildStatuses", buildStatuses);
      result.put("status", "success");
    } catch (FeignException e) {
      result.put("status", "error");
      result.put("error", extractMeaningfulErrorMessage(e));
    } catch (Exception e) {
      result.put("status", "error");
      result.put("error", "Failed to fetch build statuses: " + e.getMessage());
    }

    return CompletableFuture.completedFuture(result);
  }

  @Async("taskExecutor")
  public CompletableFuture<Map<String, Object>> getPullRequestBuildStatusesAsync(
      PrBuildRequestItem requestItem,
      String authHeader) {

    Map<String, Object> result = new HashMap<>();
    RepositoryInfo repositoryInfo = requestItem.getRepository();

    result.put("projectName", repositoryInfo.getProjectName());
    result.put("repoSlug", repositoryInfo.getRepoSlug());

    try {
      String effectiveCommitHash = requestItem.getCommitHash();
      if (effectiveCommitHash == null || effectiveCommitHash.isBlank()) {
        effectiveCommitHash = bitbucketApiService.getLatestCommitHashForPullRequest(
            repositoryInfo,
            requestItem.getPrId(),
            authHeader
        );
      }

      Map<String, Object> statuses = bitbucketApiService.getCommitBuildStatuses(
          repositoryInfo,
          effectiveCommitHash,
          authHeader
      );

      Map<String, Object> metrics = buildMetricsFromStatuses(statuses);

      Map<String, Object> buildData = new HashMap<>();
      buildData.put(effectiveCommitHash, metrics);

      result.put("buildData", buildData);
      result.put("status", "success");
    } catch (Exception e) {
      result.put("status", "error");
      result.put("error", "Failed to fetch pull request build statuses: " + e.getMessage());
    }

    return CompletableFuture.completedFuture(result);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> buildMetricsFromStatuses(Map<String, Object> statusesResponse) {
    Map<String, Object> metrics = new HashMap<>();
    int cancelled = 0;
    int successful = 0;
    int inProgress = 0;
    int failed = 0;
    int unknown = 0;

    if (statusesResponse != null) {
      Object valuesObj = statusesResponse.get("values");
      if (valuesObj instanceof List<?>) {
        for (Object o : (List<?>) valuesObj) {
          if (!(o instanceof Map<?, ?>)) {
            continue;
          }
          Map<String, Object> status = (Map<String, Object>) o;
          Object stateObj = status.get("state");
          if (!(stateObj instanceof String)) {
            unknown++;
            continue;
          }
          String state = ((String) stateObj).toUpperCase();
          switch (state) {
            case "SUCCESSFUL" -> successful++;
            case "FAILED" -> failed++;
            case "INPROGRESS" -> inProgress++;
            case "STOPPED", "CANCELLED" -> cancelled++;
            default -> unknown++;
          }
        }
      }
    }

    metrics.put("cancelled", cancelled);
    metrics.put("successful", successful);
    metrics.put("inProgress", inProgress);
    metrics.put("failed", failed);
    metrics.put("unknown", unknown);

    return metrics;
  }

}
