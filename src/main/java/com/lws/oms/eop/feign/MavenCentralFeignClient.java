package com.lws.oms.eop.feign;

import com.lws.oms.eop.config.FeignConfig;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "mavenCentralClient",
    url = "${mavencentral.api.base-url}",
    configuration = FeignConfig.class
)
public interface MavenCentralFeignClient {

  /**
   * Simple wrapper around Maven Central search API to fetch all versions
   * for a given groupId and artifactId.
   *
   * Example:
   *   q = g:"org.springframework.kafka" AND a:"spring-kafka"
   */
  @GetMapping("/solrsearch/select")
  Map<String, Object> searchArtifact(
      @RequestParam("q") String query,
      @RequestParam(value = "rows", defaultValue = "200") int rows,
      @RequestParam(value = "core", defaultValue = "gav") String core
  );
}

