package com.lws.oms.eop.feign;

import com.lws.oms.eop.config.FeignConfig;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "artifactoryClient",
    url = "${artifactory.api.base-url:http://localhost:8080}",
    configuration = FeignConfig.class
)
public interface ArtifactoryFeignClient {

  @GetMapping("/{artifactoryPath}")
  Map<String, Object> getArtifactMetadata(@PathVariable("artifactoryPath") String artifactoryPath);

}
