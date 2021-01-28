package com.googlingual.springboot.filters;


import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AuthenticationFilter implements Filter {
  private static final Logger logger = Logger.getLogger(AuthenticationFilter.class.getName());

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    String accessToken = req.getHeader("Authorization");
    logger.info("Starting a transaction for req : {}" + accessToken);
    chain.doFilter(request, response);
  }

}
