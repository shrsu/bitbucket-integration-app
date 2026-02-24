package com.lws.oms.eop.controller;

import static com.lws.oms.eop.utils.ErrorUtils.extractMeaningfulErrorMessage;

import com.lws.oms.eop.dto.DependencyApplicationMetadataDto;
import com.lws.oms.eop.service.DependencyService;
import feign.FeignException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dependencies")
@CrossOrigin(
    origins = "http://localhost:5173",
    allowedHeaders = {"Authorization", "Content-Type"},
    methods = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.OPTIONS}
)
public class DependencyApiController {

  private final DependencyService dependencyService;

  public DependencyApiController(DependencyService dependencyService) {
    this.dependencyService = dependencyService;
  }

  @GetMapping("/names")
  public ResponseEntity<List<String>> getAllDependencyNames() {
    try {
      List<String> names = dependencyService.getAllDependencyNames();
      return ResponseEntity.ok(names);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(null);
    }
  }

  @GetMapping("/{dependencyName}")
  public ResponseEntity<?> getDependentApplicationsWithMetadata(@PathVariable String dependencyName) {
    try {
      DependencyApplicationMetadataDto result =
          dependencyService.getDependencyApplicationsWithMetadata(dependencyName);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Dependency not found: " + dependencyName);
    } catch (FeignException fe) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error calling Artifactory API: " + extractMeaningfulErrorMessage(fe));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An unexpected error occurred: " + e.getMessage());
    }
  }

}
