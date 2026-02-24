package com.lws.oms.eop.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrBuildRequestItem {

  @NotNull(message = "Repository info must be provided")
  private RepositoryInfo repository;

  @NotNull(message = "Pull request ID must be provided")
  private Integer prId;

  private Integer start;
  private Integer limit;
  private Integer avatarSize;

}

