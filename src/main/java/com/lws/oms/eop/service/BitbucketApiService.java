package com.lws.oms.eop.service;

import com.lws.oms.eop.exception.CustomApiException;
import com.lws.oms.eop.feign.BitbucketFeignClient;
import com.lws.oms.eop.feign.BitbucketUiFeignClient;
import com.lws.oms.eop.model.PrInfo;
import com.lws.oms.eop.model.RepositoryInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class BitbucketApiService {

  private final BitbucketFeignClient bitbucketFeignClient;

  private final BitbucketUiFeignClient bitbucketUiFeignClient;

  private final String workspace;

  public BitbucketApiService(
      BitbucketFeignClient bitbucketFeignClient,
      BitbucketUiFeignClient bitbucketUiFeignClient,
      @org.springframework.beans.factory.annotation.Value("${bitbucket.workspace}") String workspace
  ) {
    this.bitbucketFeignClient = bitbucketFeignClient;
    this.bitbucketUiFeignClient = bitbucketUiFeignClient;
    this.workspace = workspace;
  }

  public Map<String, Object> retrieveBranches(RepositoryInfo repository, String authHeader) {
    log.info("Fetching branches for repository: {} in project: {}",
        repository.getRepoSlug(), repository.getProjectName());
    log.debug("GetBranches request - workspace: {}, repoSlug: {}", workspace, repository.getRepoSlug());

    try {
      Map<String, Object> response = bitbucketFeignClient.getBranches(
          authHeader,
          workspace,
          repository.getRepoSlug());
      log.info("Successfully fetched branches for repository: {} in project: {}",
          repository.getRepoSlug(), repository.getProjectName());
      log.debug("GetBranches response: {}", response);
      return response;
    } catch (Exception ex) {
      log.error("Error fetching branches for repository {} in project {}: {}",
          repository.getRepoSlug(), repository.getProjectName(), ex.getMessage(), ex);
      throw ex;
    }
  }

  public Map<String, Object> createBranch(
      RepositoryInfo repoInfo,
      String branchName,
      String startPoint,
      String authHeader) {

    log.info("Creating branch: {} in repo: {} from start point: {}", branchName, repoInfo.getRepoSlug(), startPoint);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", branchName);
    Map<String, Object> target = new HashMap<>();
    target.put("hash", startPoint);
    requestBody.put("target", target);

    try {
      Map<String, Object> response = bitbucketFeignClient.createBranch(
          authHeader,
          repoInfo.getProjectName(),
          repoInfo.getRepoSlug(),
          requestBody
      );
      log.info("Successfully created branch: {} in repo: {}", branchName, repoInfo.getRepoSlug());
      return response;
    } catch (Exception ex) {
      log.error("Error creating branch {} in repo {}: {}", branchName, repoInfo.getRepoSlug(), ex.getMessage(), ex);
      throw ex;
    }
  }

  public List<String> getApplications(String projectName, String authHeader) {
    log.info("Fetching applications for project: {}", projectName);

    List<String> allApplications = new ArrayList<>();
    int page = 1;
    boolean isLastPage = false;

    try {
      while (!isLastPage) {
        String query = String.format("project.key=\"%s\"", projectName);
        Map<String, Object> response = bitbucketFeignClient.getRepositories(
            authHeader,
            workspace,
            page,
            query
        );

        List<Map<String, Object>> repos = (List<Map<String, Object>>) response.get("values");
        if (!CollectionUtils.isEmpty(repos)) {
          allApplications.addAll(
              repos.stream()
                  .map(repo -> (String) repo.get("name"))
                  .collect(Collectors.toList())
          );
        }

        isLastPage = (Boolean) response.getOrDefault("isLastPage", true);
        if (!isLastPage && response.containsKey("nextPageStart")) {
          page = (Integer) response.get("nextPageStart");
        }
      }

      return allApplications;

    } catch (Exception ex) {
      log.error("Error fetching applications for project {}: {}", projectName, ex.getMessage(), ex);
      throw ex;
    }
  }

  public Map<String, Object> createPullRequest(
      PrInfo prInfo,
      RepositoryInfo repoInfo,
      String authHeader) {

    log.info("Creating pull request from: {} to: {}", prInfo.getFromBranch(), prInfo.getToBranch());

    Map<String, Object> sourceBranch = new HashMap<>();
    sourceBranch.put("name", prInfo.getFromBranch());
    Map<String, Object> source = new HashMap<>();
    source.put("branch", sourceBranch);

    Map<String, Object> destBranch = new HashMap<>();
    destBranch.put("name", prInfo.getToBranch());
    Map<String, Object> destination = new HashMap<>();
    destination.put("branch", destBranch);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("title", prInfo.getTitle());
    requestBody.put("description", prInfo.getDescription());
    requestBody.put("source", source);
    requestBody.put("destination", destination);

    try {
      Map<String, Object> response = bitbucketFeignClient.createPullRequest(
          authHeader,
          workspace,
          repoInfo.getRepoSlug(),
          requestBody
      );
      log.info("Successfully created pull request: '{}' from '{}' to '{}'",
          prInfo.getTitle(), prInfo.getFromBranch(), prInfo.getToBranch());
      return response;
    } catch (Exception ex) {
      log.error("Failed to create pull request: {}", ex.getMessage(), ex);
      throw ex;
    }
  }

  public String getLatestCommit(RepositoryInfo repositoryInfo, String branchName, String authHeader) {
    log.info("Fetching latest commit for branch: {} in repo: {} and project: {}",
        branchName, repositoryInfo.getRepoSlug(), repositoryInfo.getProjectName());

    try {
      Map<String, Object> response = bitbucketFeignClient.getBranches(
          authHeader,
          workspace,
          repositoryInfo.getRepoSlug()
      );

      List<Map<String, Object>> branches = (List<Map<String, Object>>) response.get("values");

      if (branches == null) {
        throw new CustomApiException("No branches found in repository");
      }

      for (Map<String, Object> branch : branches) {
        String name = (String) branch.get("name");
        if (branchName.equals(name)) {
          Map<String, Object> target = (Map<String, Object>) branch.get("target");
          if (target != null && target.get("hash") != null) {
            String latestCommit = (String) target.get("hash");
            log.info("Found latest commit: {}", latestCommit);
            return latestCommit;
          }
        }
      }

      throw new CustomApiException("Branch not found: " + branchName);
    } catch (Exception e) {
      log.error("Unexpected error fetching commit for branch: {}", branchName, e);
      throw e;
    }
  }

  public Map<String, Object> createCommit(
      RepositoryInfo repoInfo,
      String filePath,
      List<String> content,
      String commitMessage,
      String branchName,
      String authHeader,
      String sourceCommitId) {

    log.info("Creating commit for file: {} with commit message: {}", filePath, commitMessage);

    try {
      String contentString = String.join("\n", content) + "\n";

      return bitbucketFeignClient.createCommit(
          authHeader,
          workspace,
          repoInfo.getRepoSlug(),
          commitMessage,
          branchName,
          filePath,
          contentString
      );

    } catch (Exception ex) {
      log.error("Unexpected error while creating commit", ex);
      throw ex;
    }
  }

  public String findFilePathByName(
      RepositoryInfo repositoryInfo,
      String commit,
      String fileName,
      String authHeader,
      String moduleName,
      String searchPath) {

    String effectiveSearchPath = Stream.of(moduleName, searchPath)
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining("/"));

    String filePath = effectiveSearchPath.isEmpty()
        ? fileName
        : effectiveSearchPath + "/" + fileName;

    log.info("Using constructed file path: {} for file: {}", filePath, fileName);
    return filePath;
  }

  public List<String> getFileContent(
      RepositoryInfo repoInfo,
      String commit,
      String filePath,
      String authHeader) {

    log.info("Fetching content for file: {} at commit: {} in repo: {}", filePath, commit, repoInfo.getRepoSlug());

    try {
      String content = bitbucketFeignClient.getFileContent(
          authHeader,
          workspace,
          repoInfo.getRepoSlug(),
          commit,
          filePath,
          1
      );

      List<String> lines = new ArrayList<>();
      if (content != null && !content.isEmpty()) {
        String[] splitLines = content.split("\r?\n");
        for (String line : splitLines) {
          lines.add(line);
        }
      }

      log.info("Successfully fetched content for file: {} at commit: {} in repo: {}", filePath, commit, repoInfo.getRepoSlug());
      return lines;

    } catch (Exception ex) {
      log.error("Unexpected error fetching file content from repo {}: {}", repoInfo.getRepoSlug(), ex.getMessage(), ex);
      throw ex;
    }
  }

  public Map<String, Object> getCommitBuildStatuses(
      RepositoryInfo repoInfo,
      String commitHash,
      String authHeader) {

    log.info("Fetching build statuses for commit: {} in repo: {}", commitHash, repoInfo.getRepoSlug());

    try {
      Map<String, Object> response = bitbucketUiFeignClient.getCommitStatuses(
          authHeader,
          workspace,
          repoInfo.getRepoSlug(),
          commitHash
      );

      log.info("Successfully fetched build statuses for commit: {} in repo: {}", commitHash, repoInfo.getRepoSlug());
      return response;

    } catch (Exception ex) {
      log.error("Failed to fetch build statuses for commit {} in repo {}: {}",
          commitHash, repoInfo.getRepoSlug(), ex.getMessage(), ex);
      throw ex;
    }
  }

  /**
   * Resolve the latest commit hash for a given pull request.
   */
  public String getLatestCommitHashForPullRequest(
      RepositoryInfo repoInfo,
      Integer pullRequestId,
      String authHeader) {

    log.info(
        "Fetching latest commit hash for pull request {} in repo {}",
        pullRequestId,
        repoInfo.getRepoSlug()
    );

    try {
      Map<String, Object> response = bitbucketUiFeignClient.getPullRequestCommits(
          authHeader,
          workspace,
          repoInfo.getRepoSlug(),
          pullRequestId
      );

      List<Map<String, Object>> commits = (List<Map<String, Object>>) response.get("values");
      if (commits == null || commits.isEmpty()) {
        throw new CustomApiException(
            "No commits found for pull request " + pullRequestId + " in repo " + repoInfo.getRepoSlug()
        );
      }

      // Bitbucket returns commits in reverse chronological order; take the first as "latest".
      Map<String, Object> latestCommit = commits.get(0);
      String hash = (String) latestCommit.get("hash");
      if (hash == null || hash.isBlank()) {
        throw new CustomApiException(
            "Latest commit for pull request " + pullRequestId + " is missing a hash"
        );
      }

      log.info(
          "Resolved latest commit hash {} for pull request {} in repo {}",
          hash,
          pullRequestId,
          repoInfo.getRepoSlug()
      );
      return hash;
    } catch (Exception ex) {
      log.error(
          "Failed to resolve latest commit for pull request {} in repo {}: {}",
          pullRequestId,
          repoInfo.getRepoSlug(),
          ex.getMessage(),
          ex
      );
      throw ex;
    }
  }

}


