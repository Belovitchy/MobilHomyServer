package fr.eric.mobilhomy.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Documentation interactive de l'API (Swagger UI), en profil dev uniquement :
 * http://localhost:8080/swagger-ui.html
 * <p>
 * Pour appeler une route protégée : POST /api/auth/login, copier le jeton,
 * bouton « Authorize » puis coller le jeton (sans le préfixe Bearer).
 */
@Configuration
@Profile("dev")
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI mobilHomyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MobilHomy API")
                        .version("1.0")
                        .description("Gestion locative de mobil-homes en campings Siblu : "
                                + "vacanciers, propriétaires, gérants et administrateur."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    // Un groupe par espace / rôle (liste déroulante en haut à droite de Swagger UI)

    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder().group("1-public").pathsToMatch("/api/auth/**", "/api/mobilhomes/**").build();
    }

    @Bean
    GroupedOpenApi vacationerApi() {
        return GroupedOpenApi.builder().group("2-vacancier").pathsToMatch("/api/vacationer/**").build();
    }

    @Bean
    GroupedOpenApi ownerApi() {
        return GroupedOpenApi.builder().group("3-proprietaire").pathsToMatch("/api/owner/**").build();
    }

    @Bean
    GroupedOpenApi managerApi() {
        return GroupedOpenApi.builder().group("4-gerant").pathsToMatch("/api/manager/**").build();
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder().group("5-admin").pathsToMatch("/api/admin/**").build();
    }
}
