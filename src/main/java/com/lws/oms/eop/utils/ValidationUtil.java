package com.lws.oms.eop.utils;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

@Slf4j
public class ValidationUtil {

  public static ResponseEntity<Map<String, Object>> handleValidationErrors(BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      StringBuilder errorMessages = new StringBuilder();
      bindingResult.getAllErrors().forEach(error -> {
        errorMessages.append(error.getDefaultMessage()).append("; ");
      });

      Map<String, Object> response = new HashMap<>();
      response.put("status", "error");
      response.put("message", "Validation failed: " + errorMessages);
      log.error("Validation failed: {}", errorMessages);
      return ResponseEntity.badRequest().body(response);
    }
    return null;
  }

}
