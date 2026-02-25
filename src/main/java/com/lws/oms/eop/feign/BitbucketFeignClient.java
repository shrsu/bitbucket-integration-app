package com.lws.oms.eop.feign;

import com.lws.oms.eop.config.FeignConfig;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

@FeignClient(
    name = "bitbucketClient",
    url = "${bitbucket.api.base-url}",
    configuration = FeignConfig.class
)
public interface BitbucketFeignClient {

  /**
   * Validate credentials by fetching workspace information for the configured workspace.
   */
  @GetMapping("/workspaces/{workspace}")
  Map<String, Object> getWorkspace(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable("workspace") String workspace
  );

  /**
   * List branches for a repository in the configured workspace.
   * Bitbucket Cloud v2: /2.0/repositories/{workspace}/{repo_slug}/refs/branches
   */
  @GetMapping("/repositories/{workspace}/{repoSlug}/refs/branches")
  Map<String, Object> getBranches(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @PathVariable("workspace") String workspace,
      @PathVariable("repoSlug") String repoSlug
  );

  /**
   * Create a branch in Bitbucket Cloud.
   * Bitbucket Cloud v2: POST /2.0/repositories/{workspace}/{repo_slug}/refs/branches
   */
  @PostMapping("/repositories/{workspace}/{repoSlug}/refs/branches")
  Map<String, Object> createBranch(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @PathVariable("workspace") String workspace,
      @PathVariable("repoSlug") String repoSlug,
      @RequestBody Map<String, Object> branchInfo
  );

  /**
   * List repositories for a workspace, optionally filtered by project key.
   * Bitbucket Cloud v2: /2.0/repositories/{workspace}?page={page}&q=project.key=\"KEY\"
   */
  @GetMapping("/repositories/{workspace}")
  Map<String, Object> getRepositories(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @PathVariable("workspace") String workspace,
      @RequestParam("page") int page,
      @RequestParam("q") String projectKeyQuery
  );


  /**
   * Create a pull request in Bitbucket Cloud.
   * Bitbucket Cloud v2: POST /2.0/repositories/{workspace}/{repo_slug}/pullrequests
   */
  @PostMapping("/repositories/{workspace}/{repoSlug}/pullrequests")
  Map<String, Object> createPullRequest(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @PathVariable("workspace") String workspace,
      @PathVariable("repoSlug") String repoSlug,
      @RequestBody Map<String, Object> pullRequest
  );

  /**
   * Fetch file content for a given commit in Bitbucket Cloud.
   * Bitbucket Cloud v2: /2.0/repositories/{workspace}/{repo_slug}/src/{commit}/{path}
   */
  @GetMapping("/repositories/{workspace}/{repoSlug}/src/{commit}/{filePath}")
  String getFileContent(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @PathVariable("workspace") String workspace,
      @PathVariable("repoSlug") String repoSlug,
      @PathVariable("commit") String commit,
      @PathVariable("filePath") String filePath,
      @RequestParam(value = "page", defaultValue = "1") int page
  );

  /**
   * Create a commit by posting file content in Bitbucket Cloud.
   * Bitbucket Cloud v2: POST /2.0/repositories/{workspace}/{repo_slug}/src
   *
   * The formParts MultiValueMap MUST contain:
   *   - "message"      → commit message string
   *   - "branch"       → target branch name
   *   - "<file-path>" → the file content (key = the actual file path, e.g. "pom.xml")
   *
   * SpringFormEncoder natively supports MultiValueMap for multipart/form-data.
   */
  @PostMapping(
      value = "/repositories/{workspace}/{repoSlug}/src",
      consumes = "multipart/form-data"
  )
  Map<String, Object> createCommit(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable("workspace") String workspace,
      @PathVariable("repoSlug") String repoSlug,
      @RequestBody MultiValueMap<String, Object> formParts
  );

}

