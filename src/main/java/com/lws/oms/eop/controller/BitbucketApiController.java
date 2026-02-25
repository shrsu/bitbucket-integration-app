package com.lws.oms.eop.controller;

import static com.lws.oms.eop.utils.ErrorUtils.extractMeaningfulErrorMessage;
import static com.lws.oms.eop.utils.ValidationUtil.handleValidationErrors;

import com.lws.oms.eop.model.requests.CommitRequest;
import com.lws.oms.eop.model.requests.CreateBranchRequest;
import com.lws.oms.eop.model.requests.CreatePrRequest;
import com.lws.oms.eop.model.requests.FileContentRequest;
import com.lws.oms.eop.model.requests.GetApplicationsRequest;
import com.lws.oms.eop.model.requests.GetBranchesRequest;
import com.lws.oms.eop.model.requests.GetBuildsRequest;
import com.lws.oms.eop.model.requests.UpdateDependencyVersionRequest;
import com.lws.oms.eop.service.BitBucketIntegrationService;
import com.lws.oms.eop.service.BitbucketApiService;
import feign.FeignException;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/bitbucket")
public class BitbucketApiController {

  private final BitbucketApiService bitbucketApiService;
  private final BitBucketIntegrationService bitbucketIntegrationService;

  public BitbucketApiController(BitbucketApiService bitbucketBranchService, BitBucketIntegrationService bitbucketIntegrationService) {
    this.bitbucketApiService = bitbucketBranchService;
    this.bitbucketIntegrationService = bitbucketIntegrationService;
  }

  @PostMapping("/getBranches")
  public ResponseEntity<Map<String, Object>> getBranches(
      @CookieValue(value = "auth_token", required = false) String authToken,
      @Valid @RequestBody GetBranchesRequest request,
      BindingResult bindingResult) {

    ResponseEntity<Map<String, Object>> validationResponse = handleValidationErrors(bindingResult);
    if (validationResponse != null) {
      return validationResponse;
    }

    Map<String, Object> apiResponse = new HashMap<>();
    try {
      List<CompletableFuture<Map<String, Object>>> futures = request.getRepoList().stream()
          .map(repo -> bitbucketIntegrationService.fetchBranchDataAsync(repo, "Basic " + authToken))
          .toList();

      List<Map<String, Object>> branchesData = futures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      apiResponse.put("branchesData", branchesData);
      return ResponseEntity.ok(apiResponse);
    } catch (Exception e) {
      apiResponse.put("error", "An unexpected error occurred while processing the request: Internal Server Error");
      log.error("Unexpected error occurred: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
  }

  @PostMapping("/createBranches")
  public ResponseEntity<Map<String, Object>> createBranches(
      @CookieValue(value = "auth_token", required = false) String authToken,
      @Valid @RequestBody CreateBranchRequest request,
      BindingResult bindingResult) {

    ResponseEntity<Map<String, Object>> validationResponse = handleValidationErrors(bindingResult);
    if (validationResponse != null) {
      return validationResponse;
    }

    Map<String, Object> response = new HashMap<>();
    try {
      List<CompletableFuture<Map<String, Object>>> futures = request.getRepoList().stream()
          .map(repoInfo -> bitbucketIntegrationService.createBranchAsync(
              repoInfo,
              request.getBranchName(),
              request.getStartPoint(),
              "Basic " + authToken
          ))
          .toList();

      List<Map<String, Object>> branchResults = futures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      response.put("branchResults", branchResults);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.put("error", "An unexpected error occurred while processing the request: Internal Server Error");
      log.error("Unexpected error occurred while creating branches: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/getApplications")
  public ResponseEntity<Map<String, Object>> getApplications(
      @CookieValue(value = "auth_token", required = false) String authToken,
      @Valid @RequestBody GetApplicationsRequest request,
      BindingResult bindingResult) {

    ResponseEntity<Map<String, Object>> validationResponse = handleValidationErrors(bindingResult);
    if (validationResponse != null) {
      return validationResponse;
    }

    Map<String, Object> apiResponse = new HashMap<>();
    try {
      List<String> applications = bitbucketApiService.getApplications(request.getProjectName(), "Basic " + authToken);
      apiResponse.put("applications", applications);
    } catch (FeignException e) {
      apiResponse.put("error", extractMeaningfulErrorMessage(e));
    } catch (Exception e) {
      apiResponse.put("error", "Error fetching applications: Server Error");
      log.error("Unexpected error fetching applications: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }

    return ResponseEntity.ok(apiResponse);
  }

  @PostMapping("/createPullRequests")
  public ResponseEntity<Map<String, Object>> createPullRequests(
      @CookieValue(value = "auth_token", required = false) String authToken,
      @Valid @RequestBody CreatePrRequest request,
      BindingResult bindingResult) {

    ResponseEntity<Map<String, Object>> validationResponse = handleValidationErrors(bindingResult);
    if (validationResponse != null) {
      return validationResponse;
    }

    Map<String, Object> response = new HashMap<>();

    try {
      List<CompletableFuture<Map<String, Object>>> futures = request.getPrInfoList().stream()
          .flatMap(prInfo -> prInfo.getRepoInfoList().stream()
              .map(repoInfo -> bitbucketIntegrationService.createPullRequestAsync(prInfo, repoInfo, "Basic " + authToken)))
          .toList();

      List<Map<String, Object>> prResults = futures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      response.put("createPullRequestResults", prResults);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      response.put("message", "An unexpected error occurred: Internal Server Error");
      log.error("Unexpected error occurred during creating Pull requests: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/getFileContents")
  public ResponseEntity<Map<String, Object>> getFileContents(
      @CookieValue(value = "auth_token", required = false) String authToken,
      @Valid @RequestBody FileContentRequest request,
      BindingResult bindingResult) {

    ResponseEntity<Map<String, Object>> validationResponse = handleValidationErrors(bindingResult);
    if (validationResponse != null) {
      return validationResponse;
    }

    Map<String, Object> response = new HashMap<>();

    try {
      List<CompletableFuture<Map<String, Object>>> futures = request.getFiles().stream()
          .map(fileInfo -> bitbucketIntegrationService.getFileContentAsync(
              fileInfo,
              request.getBranchName(),
              "Basic " + authToken))
          .toList();

      List<Map<String, Object>> fileContents = futures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      response.put("fileContents", fileContents);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      response.put("error", "An unexpected error occurred while processing the request: Internal Server Error");
      log.error("Unexpected error occurred while getting file contents: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/createCommits")
  public ResponseEntity<Map<String, Object>> createCommit(
      @CookieValue(value = "auth_token", required = false) String authToken,
      @Valid @RequestBody CommitRequest commitRequest,
      BindingResult bindingResult) {

    ResponseEntity<Map<String, Object>> validationResponse = handleValidationErrors(bindingResult);
    if (validationResponse != null) {
      return validationResponse;
    }

    Map<String, Object> response = new HashMap<>();
    try {
      List<CompletableFuture<Map<String, Object>>> futures = commitRequest.getCommitInfoList().stream()
          .map(commitInfo -> bitbucketIntegrationService.createCommitAsync(commitInfo, commitRequest, "Basic " + authToken))
          .toList();

      List<Map<String, Object>> commitResponses = futures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      response.put("commitResults", commitResponses);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.put("message", "An unexpected error occurred: Internal Server Error");
      log.error("Unexpected error occurred during commit creation: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/updateDependencyVersion")
  public ResponseEntity<Map<String, Object>> updateDependencyVersion(
      @CookieValue(value = "auth_token", required = false) String authToken,
      @Valid @RequestBody UpdateDependencyVersionRequest request,
      BindingResult bindingResult) {

    ResponseEntity<Map<String, Object>> validationResponse = handleValidationErrors(bindingResult);
    if (validationResponse != null) {
      return validationResponse;
    }

    Map<String, Object> response = new HashMap<>();
    try {
      List<CompletableFuture<Map<String, Object>>> futures = request.getRepoList().stream()
          .map(repoInfo -> bitbucketIntegrationService.updateDependencyVersionAsync(repoInfo, request, "Basic " + authToken))
          .toList();

      List<Map<String, Object>> updatedFiles = futures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      response.put("updatedResults", updatedFiles);
    } catch (Exception e) {
      response.put("message", "An unexpected error occurred while processing the request: " + e.getMessage());
      log.error("Unexpected error occurred while updating dependency version: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    return ResponseEntity.ok(response);
  }

  @PostMapping("/getCommitBuildStatuses")
  public ResponseEntity<Map<String, Object>> getCommitBuildStatuses(
      @CookieValue(value = "auth_token", required = false) String authToken,
      @Valid @RequestBody GetBuildsRequest request,
      BindingResult bindingResult) {

    ResponseEntity<Map<String, Object>> validationResponse = handleValidationErrors(bindingResult);
    if (validationResponse != null) {
      return validationResponse;
    }

    Map<String, Object> response = new HashMap<>();
    try {
      List<CompletableFuture<Map<String, Object>>> futures = request.getRequestItems().stream()
          .map(item -> bitbucketIntegrationService.getCommitBuildStatusesAsync(item.getRepository(), item.getCommitHash(), "Basic " + authToken))
          .toList();

      List<Map<String, Object>> buildStatuses = futures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      response.put("commitBuildStatuses", buildStatuses);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      response.put("message", "An unexpected error occurred while fetching build statuses: " + e.getMessage());
      log.error("Unexpected error occurred while fetching build statuses for commits: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/getPullRequestBuilds")
  public ResponseEntity<Map<String, Object>> getPullRequestBuilds(
      @CookieValue(value = "auth_token", required = false) String authToken,
      @Valid @RequestBody GetBuildsRequest request,
      BindingResult bindingResult) {

    ResponseEntity<Map<String, Object>> validationResponse = handleValidationErrors(bindingResult);
    if (validationResponse != null) {
      return validationResponse;
    }

    Map<String, Object> response = new HashMap<>();
    try {
      List<CompletableFuture<Map<String, Object>>> futures = request.getRequestItems().stream()
          .map(item -> bitbucketIntegrationService.getPullRequestBuildStatusesAsync(
              item,
              "Basic " + authToken
          ))
          .toList();

      List<Map<String, Object>> buildResults = futures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      response.put("pullRequestBuilds", buildResults);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.put("message",
          "An unexpected error occurred while fetching pull request build statuses: " + e.getMessage());
      log.error("Unexpected error occurred while fetching pull request build statuses: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

}

