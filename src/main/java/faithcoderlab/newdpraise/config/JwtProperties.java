package faithcoderlab.newdpraise.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class JwtProperties {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.access-token-expiration}")
  private long accessTokenExpiration;

  @Value("${jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  @Value("${jwt.token-prefix}")
  private String tokenPrefix;

  @Value("${jwt.header-string}")
  private String headerString;
}
