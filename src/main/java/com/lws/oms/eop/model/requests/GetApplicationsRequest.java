package com.lws.oms.eop.model.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GetApplicationsRequest {

  @NotBlank(message = "Project name must not be blank")
  private String projectName;

}
