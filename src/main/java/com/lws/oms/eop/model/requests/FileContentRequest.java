package com.lws.oms.eop.model.requests;

import com.lws.oms.eop.model.FileInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class FileContentRequest {

  @NotNull(message = "Files Information cannot be null")
  private List<FileInfo> files;

  @NotBlank(message = "Branch name cannot be blank")
  private String branchName;

}
