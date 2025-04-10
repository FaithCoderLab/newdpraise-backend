package faithcoderlab.newdpraise.global.security;

import faithcoderlab.newdpraise.config.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final UserDetailsService userDetailsService;
  private final JwtProperties jwtProperties;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    final String authHeader = request.getHeader(jwtProperties.getHeaderString());
    final String tokenPrefix = jwtProperties.getTokenPrefix();

    String username = null;
    String jwt = null;

    if (authHeader != null && authHeader.startsWith(tokenPrefix)) {
      jwt = authHeader.substring(tokenPrefix.length());
      try {
        username = jwtProvider.extractUsername(jwt);
      } catch (ExpiredJwtException e) {
        log.debug("만료된 JWT 토큰입니다: {}", e.getMessage());
      } catch (UnsupportedJwtException e) {
        log.debug("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
      } catch (MalformedJwtException e) {
        log.debug("JWT 토큰이 올바르게 구성되지 않았습니다: {}", e.getMessage());
      } catch (SignatureException e) {
        log.debug("JWT 서명 검증에 실패했습니다: {}", e.getMessage());
      } catch (IllegalArgumentException e) {
        log.debug("JWT 클레임 문자열이 비어 있습니다: {}", e.getMessage());
      } catch (Exception e) {
        log.error("JWT 토큰 처리 중 예상치 못한 오류가 발생했습니다", e);
      }
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

      if (jwtProvider.validateToken(jwt, userDetails)) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()
        );

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    filterChain.doFilter(request, response);
  }
}
