package cz.levy.pet.shelter.aggregator.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import cz.levy.pet.shelter.aggregator.dto.DogDto;
import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import cz.levy.pet.shelter.aggregator.entity.ShelterEntity;
import cz.levy.pet.shelter.aggregator.mapper.DogMapper;
import cz.levy.pet.shelter.aggregator.repository.DogRepository;
import cz.levy.pet.shelter.aggregator.repository.ShelterRepository;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.testcontainers.containers.PostgreSQLContainer;

@Configuration
@Profile("local-simulated")
public class LocalSimulatedConfig implements WebMvcConfigurer {

  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:14");

  @Bean
  public PostgreSQLContainer<?> postgresContainer() {
    POSTGRES.start();
    return POSTGRES;
  }

  @Bean
  @DependsOn("postgresContainer")
  public DataSource dataSource() {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(POSTGRES.getJdbcUrl());
    ds.setUsername(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());
    ds.setDriverClassName(POSTGRES.getDriverClassName());
    return ds;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Compute absolute path to frontend/build
    String userDir = System.getProperty("user.dir");
    Path buildPath = Paths.get(userDir).resolve("frontend/build").normalize().toAbsolutePath();
    String buildLocation = "file:" + buildPath + "/";

    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/", buildLocation)
        .setCachePeriod(0)
        .resourceChain(true)
        .addResolver(new PathResourceResolver());
  }

  @Bean
  public SmartLifecycle nodeProxyLifecycle() {
    return new SmartLifecycle() {
      private Process process;
      private boolean running = false;

      @Override
      public void start() {
        try {
          String userDir = System.getProperty("user.dir");
          Path frontendPath =
              Paths.get(userDir)
                  .resolve("frontend") // adjust relative levels
                  .normalize()
                  .toAbsolutePath();

          ProcessBuilder pb;
          String os = System.getProperty("os.name").toLowerCase();
          if (os.contains("win")) {
            pb = new ProcessBuilder("cmd", "/c", "npm.cmd", "run", "dev");
          } else {
            pb = new ProcessBuilder("npm", "run", "dev");
          }
          pb.directory(frontendPath.toFile());
          pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
          pb.redirectError(ProcessBuilder.Redirect.INHERIT);
          process = pb.start();
          running = true;
        } catch (IOException e) {
          throw new RuntimeException("Failed to start React and Node dev servers", e);
        }
      }

      @Override
      public void stop() {
        if (process != null && process.isAlive()) {
          process.destroyForcibly();
        }
        running = false;
      }

      @Override
      public boolean isRunning() {
        return running;
      }

      @Override
      public int getPhase() {
        return Integer.MAX_VALUE - 1;
      }

      @Override
      public boolean isAutoStartup() {
        return true;
      }

      @Override
      public void stop(Runnable callback) {
        stop();
        callback.run();
      }
    };
  }

  @Bean
  @DependsOn("dataSource")
  public CommandLineRunner loadSampleData(
      DogRepository dogRepository, ShelterRepository shelterRepository) {
    return args -> {
      var shelter =
          shelterRepository.save(
              ShelterEntity.builder()
                  .name("PesWeb.cz")
                  .url("https://www.pesweb.cz/cz/psi-k-adopci")
                  .isNonProfit(true)
                  .build());
      ObjectMapper mapper = new ObjectMapper();
      try (InputStream is = getClass().getResourceAsStream("/dogs.json")) {
        List<DogDto> dtos = mapper.readValue(is, new TypeReference<>() {});

        List<DogEntity> entities =
            dtos.stream()
                .map(dogDto -> DogMapper.dtoToEntity(dogDto, shelter))
                .collect(Collectors.toList());

        dogRepository.saveAll(entities);
        System.out.println("Loaded " + entities.size() + " dogs from JSON");
      }
    };
  }

  @Component
  @Profile("local-simulated")
  public class BrowserOpener implements SmartLifecycle {
    private boolean running = false;

    @Value("${server.port:8080}")
    private int serverPort;

    @Override
    public void start() {
      try {
        String url = "http://localhost:" + serverPort + "/";
        if (Desktop.isDesktopSupported()) {
          Desktop.getDesktop().browse(new URI(url));
          running = true;
        } else {
          System.out.println("Please open " + url + " in your browser.");
        }
      } catch (Exception e) {
        System.err.println("Failed to open browser: " + e.getMessage());
      }
    }

    @Override
    public void stop() {
      running = false;
    }

    @Override
    public boolean isRunning() {
      return running;
    }

    @Override
    public int getPhase() {
      return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
      return true;
    }

    @Override
    public void stop(Runnable callback) {
      running = false;
      callback.run();
    }
  }
}
