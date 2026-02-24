package com.lws.oms.eop.model.requests;

import com.lws.oms.eop.model.PrBuildRequestItem;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetBuildsRequest {

  @NotEmpty(message = "Request items cannot be empty")
  private List<PrBuildRequestItem> requestItems;

}
