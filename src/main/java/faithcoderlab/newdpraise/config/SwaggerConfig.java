package faithcoderlab.newdpraise.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI openAPI() {
    Info info = new Info()
        .title("NewDpraise API")
        .version("1.0.0")
        .description("공연 콘티 관련 서비스 API 문서");

    String securitySchemeName = "bearerAuth";
    SecurityScheme securityScheme = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .in(SecurityScheme.In.HEADER)
        .name("Authorization");

    SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

    return new OpenAPI()
        .info(info)
        .components(new Components().addSecuritySchemes(securitySchemeName, securityScheme))
        .addSecurityItem(securityRequirement);
  }
}
