package com.lws.oms.eop.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class ErrorUtils {

  public static String extractMeaningfulErrorMessage(FeignException ex) {
    try {
      String errorResponse = ex.contentUTF8();
      log.info("Raw error response: {}", errorResponse);

      ObjectMapper objectMapper = new ObjectMapper();

      int jsonStart = errorResponse.indexOf("{");
      int jsonEnd = errorResponse.lastIndexOf("}");

      if (jsonStart == -1 || jsonEnd == -1 || jsonStart >= jsonEnd) {
        return getFallbackMessage(ex.status());
      }

      String jsonString = errorResponse.substring(jsonStart, jsonEnd + 1);
      JsonNode errorJson = objectMapper.readTree(jsonString);

      // Bitbucket Cloud format: { "error": { "message": "...", "code": "..." } }
      if (errorJson.has("error")) {
        JsonNode errorNode = errorJson.get("error");
        if (errorNode.has("message") && !errorNode.get("message").asText().isBlank()) {
          return errorNode.get("message").asText();
        }
      }

      // Bitbucket Server / other format: { "errors": [ { "message": "..." } ] }
      if (errorJson.has("errors")) {
        JsonNode errors = errorJson.get("errors");
        if (errors.isArray() && !errors.isEmpty() && errors.get(0).has("message")) {
          return errors.get(0).get("message").asText();
        }
      }

    } catch (Exception parseEx) {
      log.error("Failed to extract detailed error message from API response", parseEx);
    }

    return getFallbackMessage(ex.status());
  }

  private static String getFallbackMessage(int statusCode) {
    if (statusCode == HttpStatus.NOT_FOUND.value()) {
      return "Resource not found.";
    } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
      return "Authentication failed. Please check your credentials.";
    } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
      return "You do not have permission to perform this action.";
    } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
      return "Invalid request. Please check the input parameters.";
    } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
      return "Server encountered an unexpected error. Please try again later.";
    } else if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
      return "Service is temporarily unavailable. Please try again later.";
    } else if (statusCode == -1) {
      return "Connection timed out, please try again later.";
    }

    return "An unexpected error occurred. HTTP Status: " + statusCode;
  }

}
