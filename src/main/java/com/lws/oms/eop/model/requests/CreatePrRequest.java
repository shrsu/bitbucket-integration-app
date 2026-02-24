package com.lws.oms.eop.model.requests;

import com.lws.oms.eop.model.PrInfo;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class CreatePrRequest {

  @NotNull(message = "Pr Info List cannot be null")
  private List<PrInfo> prInfoList;

}
