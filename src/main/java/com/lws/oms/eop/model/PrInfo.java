package com.lws.oms.eop.model;

import java.util.List;
import lombok.Data;

@Data
public class PrInfo {

  private List<RepositoryInfo> repoInfoList;
  private String fromBranch;
  private String toBranch;
  private String title;
  private String description;

}
