package com.lws.oms.eop.model.requests;

import com.lws.oms.eop.model.RepositoryInfo;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class GetBranchesRequest {

  @NotEmpty
  private List<RepositoryInfo> repoList;

}
