package com.lws.oms.eop.controller;

import com.lws.oms.eop.feign.BitbucketFeignClient;
import feign.FeignException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@CrossOrigin(
    origins = "http://localhost:5173",
    allowedHeaders = {"Authorization", "Content-Type"},
    methods = {org.springframework.web.bind.annotation.RequestMethod.POST, org.springframework.web.bind.annotation.RequestMethod.OPTIONS},
    allowCredentials = "true"
)
@Slf4j
public class AuthValidationController {

  private final BitbucketFeignClient bitbucketFeignClient;
  private final String workspace;
  private final boolean cookieSecure;

  public AuthValidationController(BitbucketFeignClient bitbucketFeignClient,
      @Value("${bitbucket.workspace}") String workspace,
      @Value("${app.cookies.secure:false}") boolean cookieSecure) {
    this.bitbucketFeignClient = bitbucketFeignClient;
    this.workspace = workspace;
    this.cookieSecure = cookieSecure;
  }

  @PostMapping("/validate")
  public ResponseEntity<Map<String, Object>> validateCredentials(
      @RequestHeader("Authorization") String authHeader) {

    Map<String, Object> response = new HashMap<>();

    try {
      logAuthHeaderDiagnostics("validate", authHeader);
      log.info("Validating Bitbucket credentials for workspace='{}'", workspace);
      log.info("Authorization header length={}", authHeader != null ? authHeader.length() : 0);

      // Forward Authorization header as-is (supports Basic / Bearer)
      bitbucketFeignClient.getWorkspace(authHeader, workspace);

      // Cookies cannot contain spaces; store only the token part (without "Basic ")
      String cookieValue = authHeader != null ? authHeader : "";
      if (cookieValue.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
        cookieValue = cookieValue.substring("Basic ".length()).trim();
      }

      ResponseCookie tokenCookie = ResponseCookie.from("auth_token", cookieValue)
          .httpOnly(true).secure(cookieSecure).path("/").sameSite("Strict").maxAge(60 * 30)
          .build();

      response.put("status", "valid");

      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, tokenCookie.toString())
          .body(response);

    } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
      log.warn(
          "Bitbucket credentials rejected for workspace='{}' (status={}): {}{}",
          workspace,
          e.status(),
          e.getMessage(),
          safeFeignBodySuffix(e)
      );
      response.put("status", "invalid");
      response.put("error", "Invalid credentials");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    } catch (Exception e) {
      log.error("Unexpected error while validating credentials for workspace='{}'", workspace, e);
      response.put("status", "error");
      response.put("error", "Unexpected error validating credentials");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/check")
  public ResponseEntity<Map<String, Object>> checkToken(
      @CookieValue(value = "auth_token", required = false) String authToken) {

    Map<String, Object> response = new HashMap<>();

    if (authToken == null || authToken.isBlank()) {
      log.info("Auth token missing or blank in /auth/check");
      response.put("status", "invalid");
      response.put("error", "Missing auth token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    try {
      // Cookie stores only the token; reconstruct the Authorization header
      String reconstructedHeader = authToken;
      if (!reconstructedHeader.regionMatches(true, 0, "Basic ", 0, "Basic ".length())
          && !reconstructedHeader.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
        reconstructedHeader = "Basic " + reconstructedHeader;
      }

      logAuthHeaderDiagnostics("check", reconstructedHeader);
      log.info("Checking stored Bitbucket auth token for workspace='{}'", workspace);
      log.info("Auth token length={}", reconstructedHeader.length());

      bitbucketFeignClient.getWorkspace(reconstructedHeader, workspace);
      response.put("status", "valid");
      return ResponseEntity.ok(response);

    } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
      ResponseCookie cleared = ResponseCookie.from("auth_token", "")
          .httpOnly(true).secure(cookieSecure).path("/").sameSite("Strict").maxAge(0)
          .build();

      log.warn(
          "Stored Bitbucket auth token rejected for workspace='{}' (status={}): {}{}",
          workspace,
          e.status(),
          e.getMessage(),
          safeFeignBodySuffix(e)
      );
      response.put("status", "invalid");
      response.put("error", "Invalid token");

      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header(HttpHeaders.SET_COOKIE, cleared.toString())
          .body(response);

    } catch (Exception e) {
      log.error("Unexpected error while validating stored token for workspace='{}'", workspace, e);
      response.put("status", "error");
      response.put("error", "Unexpected error validating token");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout() {
    log.info("Processing logout request - clearing auth_token cookie");
    ResponseCookie cleared = ResponseCookie.from("auth_token", "")
        .httpOnly(true)
        .secure(cookieSecure)
        .path("/")
        .sameSite("None")
        .maxAge(0)
        .build();

    Map<String, Object> response = new HashMap<>();
    response.put("status", "logged_out");

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cleared.toString())
        .body(response);
  }

  private void logAuthHeaderDiagnostics(String endpoint, String maybeBase64Token) {
    if (maybeBase64Token == null) {
      log.info("Auth header missing in /auth/{}", endpoint);
      return;
    }

    String token = maybeBase64Token.trim();
    if (token.isEmpty()) {
      log.info("Auth header blank in /auth/{}", endpoint);
      return;
    }

    if (token.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
      log.info(
          "Bearer token received for /auth/{} (len={})",
          endpoint,
          token.length()
      );
      return;
    }

    if (token.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
      log.warn("Client sent 'Basic ' prefix to /auth/{}; UI should send only base64", endpoint);
      token = token.substring("Basic ".length()).trim();
    }

    try {
      String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
      int idx = decoded.indexOf(':');
      String username = idx >= 0 ? decoded.substring(0, idx) : decoded;
      int passwordLen = idx >= 0 ? decoded.length() - idx - 1 : 0;

      log.info(
          "Auth header decoded for /auth/{}: username='{}' (len={}), passwordLen={}",
          endpoint,
          username,
          username.length(),
          passwordLen
      );
    } catch (IllegalArgumentException ex) {
      log.warn("Auth header for /auth/{} is not valid base64 (len={})", endpoint, token.length());
    }
  }

  private String safeFeignBodySuffix(FeignException e) {
    try {
      String body = e.contentUTF8();
      if (body == null) {
        return "";
      }
      body = body.trim();
      if (body.isEmpty()) {
        return "";
      }
      int max = 500;
      String truncated = body.length() > max ? body.substring(0, max) + "...(truncated)" : body;
      return " body=" + truncated;
    } catch (Exception ignored) {
      return "";
    }
  }
}
