package com.lws.oms.eop.model;

import java.util.List;
import lombok.Data;

@Data
public class CommitInfo {

  private RepositoryInfo repositoryInfo;
  private String fileName;
  private String searchPath;
  private String commitMessage;
  private List<String> content;

}
