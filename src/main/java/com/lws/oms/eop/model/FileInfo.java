package com.lws.oms.eop.model;

import lombok.Data;

@Data
public class FileInfo {

  private RepositoryInfo repositoryInfo;
  private String fileName;
  private String searchPath;

}
