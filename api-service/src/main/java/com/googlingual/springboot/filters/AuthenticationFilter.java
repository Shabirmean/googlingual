package com.googlingual.springboot.filters;


import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.io.IOException;
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
  private static final String SPACE = " ";
  private static final String BEARER_TOKEN = "Bearer";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final FirebaseApp FIREBASE_APP = FirebaseApp.initializeApp();

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    try {
      // if its an OPTIONS request then let it pass through
      if (httpRequest.getMethod().equals(HttpMethod.OPTIONS.toString())) {
        chain.doFilter(request, response);
        return;
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
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
      if (decodedToken != null) {
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        chain.doFilter(request, response);
      } else {
        logger.warning("<FAILED AUTHENTICATION> Invalid Google ID token. [" + authorizationHeader + "]");
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      }
    } catch (IOException | FirebaseAuthException e) {
      logger.warning(e.getLocalizedMessage());
      httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
