package com.googlingual.springboot.filters;


import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.googlingual.springboot.api.TranslateApi;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AuthenticationFilter implements Filter {

  private static final Logger logger = Logger.getLogger(AuthenticationFilter.class.getName());
  private static final JacksonFactory jacksonFactory = new JacksonFactory();
  private static final String SPACE = " ";
  private static final String BEARER_TOKEN = "Bearer";
  private static final String SECRET_VERSION = "latest";
  private static final String PING_HEADER = "ping";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String ACCOUNTS_GOOGLE_COM = "accounts.google.com";
  private static final String OAUTH_CLIENT_ID_SECRET_KEY = System
      .getenv("OAUTH_CLIENT_ID_SECRET_KEY");
  private static GoogleIdTokenVerifier verifier;

  private GoogleIdTokenVerifier getTokenVerifier() throws IOException {
    if (verifier == null) {
      try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
        SecretVersionName secret = SecretVersionName.of(
            TranslateApi.PROJECT_GCLOUD_DPE, OAUTH_CLIENT_ID_SECRET_KEY, SECRET_VERSION);
        String clientId = client.accessSecretVersion(secret).getPayload().getData().toStringUtf8();
        verifier = new GoogleIdTokenVerifier.Builder(
            UrlFetchTransport.getDefaultInstance(), jacksonFactory)
            .setAudience(Collections.singletonList(clientId))
            .setIssuer(ACCOUNTS_GOOGLE_COM)
            .build();
      }
    }
    return verifier;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    try {
      logger.info("Received request with HTTP METHOD: " + httpRequest.getMethod());
      // if its an OPTIONS request then let it pass through
      if (httpRequest.getMethod().equals(HttpMethod.OPTIONS.toString())) {
        chain.doFilter(request, response);
        return;
      }
      // if its a keep alive message then let it pass through without auth check
      String pingHeader = httpRequest.getHeader(PING_HEADER);
      if (StringUtils.isNotBlank(pingHeader) && pingHeader.equals("keep-alive")) {
        chain.doFilter(request, response);
      }
      String authorizationHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);
      authorizationHeader = authorizationHeader == null ?
          httpRequest.getHeader(AUTHORIZATION_HEADER.toLowerCase()) : authorizationHeader;
      if (StringUtils.isBlank(authorizationHeader)) {
        logger.warning("Authorization header not found");
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      String[] tokenParts = authorizationHeader.split(SPACE);
      if (tokenParts.length != 2 || !tokenParts[0].equals(BEARER_TOKEN)) {
        logger.warning(String.format("Invalid Authorization header [%s]", authorizationHeader));
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }

      String token = tokenParts[1];
      GoogleIdTokenVerifier verifier = getTokenVerifier();
      GoogleIdToken idToken = verifier.verify(token);
      if (idToken != null) {
        GoogleIdToken.Payload payload = idToken.getPayload();
        String userId = payload.getSubject();
        String email = payload.getEmail();
        logger.info(String.format("Request from user [id: %s] and [email: %s]", userId, email));
        chain.doFilter(request, response);
      } else {
        logger.warning("Invalid Google ID token. [" + authorizationHeader + "]");
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      }
    } catch (GeneralSecurityException | IOException e) {
      logger.warning(e.getLocalizedMessage());
      httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
