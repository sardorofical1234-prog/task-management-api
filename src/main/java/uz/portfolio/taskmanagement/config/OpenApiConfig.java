package uz.portfolio.taskmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Management API")
                        .version("v1")
                        .description("""
                                REST API for creating, searching and managing tasks.
                                The current milestone provides CRUD operations, pagination,
                                sorting, filtering and status updates.
                                """));
    }
}
