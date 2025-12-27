package cz.levy.pet.shelter.aggregator.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private static final String API_KEY_HEADER = "X-API-Key";
  private final String expectedApiKey;

  public ApiKeyAuthenticationFilter(String expectedApiKey) {
    this.expectedApiKey = expectedApiKey;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Only apply to /dogs/reportUnavailable endpoint
    if (request.getRequestURI().equals("/dogs/reportUnavailable")) {
      String apiKey = request.getHeader(API_KEY_HEADER);

      if (apiKey != null && apiKey.equals(expectedApiKey) && !expectedApiKey.isEmpty()) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                "proxy", null, AuthorityUtils.createAuthorityList("ROLE_PROXY"));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
      // If API key doesn't match, authentication will remain null and Spring Security will reject
    }

    filterChain.doFilter(request, response);
  }
}

