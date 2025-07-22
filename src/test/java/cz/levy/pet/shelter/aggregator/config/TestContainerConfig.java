package cz.levy.pet.shelter.aggregator.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestConfiguration
public class TestContainerConfig {

  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    PostgreSQLContainer<?> container =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");
    container.start();   // make sure it’s running by the time Spring wires up the DataSource
    return container;
  }

}

