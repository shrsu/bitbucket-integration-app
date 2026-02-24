package com.lws.oms.eop.model.requests;

import com.lws.oms.eop.model.RepositoryInfo;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class CreateBranchRequest {

  @NotNull(message = "Repo list cannot be null")
  private List<RepositoryInfo> repoList;

  @NotNull(message = "Branch name cannot be blank")
  private String branchName;

  @NotNull(message = "Start point cannot be blank")
  private String startPoint;

}

