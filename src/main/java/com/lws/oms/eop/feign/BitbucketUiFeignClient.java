package com.lws.oms.eop.feign;

import com.lws.oms.eop.config.FeignConfig;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "bitbucketUiClient",
    url = "${bitbucket.ui.base-url}",
    configuration = FeignConfig.class
)
public interface BitbucketUiFeignClient {

  @GetMapping("/projects/{projectKey}/repos/{repoSlug}/pull-requests/{pullRequestId}/build-summaries")
  Map<String, Object> getPullRequestBuilds(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @PathVariable("projectKey") String projectKey,
      @PathVariable("repoSlug") String repoSlug,
      @PathVariable("pullRequestId") int pullRequestId
  );

  @GetMapping("/projects/{projectKey}/repos/{repoSlug}/pull-requests/{pullRequestId}/builds")
  Map<String, Object> getPullRequestBuildList(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
      @PathVariable("projectKey") String projectKey,
      @PathVariable("repoSlug") String repoSlug,
      @PathVariable("pullRequestId") int pullRequestId,
      @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader,
      @RequestParam("start") int start,
      @RequestParam("limit") int limit,
      @RequestParam("avatarSize") int avatarSize
  );
}

