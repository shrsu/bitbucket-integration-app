package com.lws.oms.eop.feign;

import com.lws.oms.eop.config.FeignConfig;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "bitbucketUiClient",
    url = "${bitbucket.api.base-url}",
    configuration = FeignConfig.class
)
public interface BitbucketUiFeignClient {

  /**
   * Get commit build statuses (Bitbucket Cloud way of tracking builds).
   * Bitbucket Cloud v2: GET /repositories/{workspace}/{repo_slug}/commit/{commit_hash}/statuses
   */
  @GetMapping("/repositories/{workspace}/{repoSlug}/commit/{commitHash}/statuses")
  Map<String, Object> getCommitStatuses(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @PathVariable("workspace") String workspace,
      @PathVariable("repoSlug") String repoSlug,
      @PathVariable("commitHash") String commitHash
  );

  /**
   * List commits for a pull request.
   * Bitbucket Cloud v2: GET /repositories/{workspace}/{repo_slug}/pullrequests/{pull_request_id}/commits
   */
  @GetMapping("/repositories/{workspace}/{repoSlug}/pullrequests/{pullRequestId}/commits")
  Map<String, Object> getPullRequestCommits(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @PathVariable("workspace") String workspace,
      @PathVariable("repoSlug") String repoSlug,
      @PathVariable("pullRequestId") Integer pullRequestId
  );

}

