package prototype.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lineageOpenApi() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Lineage Simulation API")
                        .version("0.1.0")
                        .description("""
                                APIs for orchestrating lineage-aware digital life simulations, \
                                runtime checkpoints, and autonomous recovery workflows.
                                """)
                        .contact(new Contact()
                                .name("Lineage Runtime Team")
                                .email("ops@lineage-sim.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local development"));
    }
}


