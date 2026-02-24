package com.lws.oms.eop.model.requests;

import com.lws.oms.eop.model.RepositoryInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateDependencyVersionRequest {

  @NotNull(message = "Repo list cannot be null")
  private List<RepositoryInfo> repoList;

  @NotBlank(message = "Branch name cannot be blank")
  private String branchName;

  @NotBlank(message = "Dependency name cannot be blank")
  private String dependency;

  @NotBlank(message = "Version cannot be blank")
  private String version;

}
