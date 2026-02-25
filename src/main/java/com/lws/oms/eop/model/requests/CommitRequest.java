package com.lws.oms.eop.model.requests;

import com.lws.oms.eop.model.CommitInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class CommitRequest {

  @NotNull(message = "Commit Information List cannot be null")
  private List<CommitInfo> commitInfoList;

  @NotBlank(message = "Ticket number cannot be blank")
  private String ticketNumber;

  @NotBlank(message = "Branch name cannot be blank")
  private String branch;

}
