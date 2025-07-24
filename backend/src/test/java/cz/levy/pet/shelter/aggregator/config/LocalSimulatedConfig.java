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
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.awt.*;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Profile("local-simulated")
public class LocalSimulatedConfig implements WebMvcConfigurer {


  private static final PostgreSQLContainer<?> POSTGRES =
          new PostgreSQLContainer<>("postgres:14");

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
    registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/");
  }

  /**
   * Instead of running SQL init, load dogs.json and persist via repository
   */
  @Bean
  @DependsOn("dataSource")
  public CommandLineRunner loadSampleData(DogRepository dogRepository, ShelterRepository shelterRepository) {
    return args -> {
      var shelter = shelterRepository.save(ShelterEntity.builder().name("PesWeb.cz")
                      .url("https://www.pesweb.cz/cz/psi-k-adopci")
                      .isNonProfit(true)
              .build());
      ObjectMapper mapper = new ObjectMapper();
      try (InputStream is =
                   getClass().getResourceAsStream("/dogs.json")) {
        List<DogDto> dtos =
                mapper.readValue(is, new TypeReference<>(){});

        List<DogEntity> entities = dtos.stream().map(dogDto -> DogMapper.dtoToEntity(dogDto, shelter))
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

    @SneakyThrows
    @Override
    public void start() {
      String url = "http://localhost:" + serverPort + "/index.html";
      if (openedChrome(url)) return;
      if (openedEdge(url)) return;
      if (openedDefaultBrowser(url)) return;
      else System.out.println("Open " + url + " in your browser.");
      running = true;
    }

    private boolean openedChrome(String url) {
      try {
        new ProcessBuilder("cmd", "/c", "start", "chrome", url).start();
        running = true;
        return true;
      } catch (Exception ignored) {
        return false;
      }
    }

    private boolean openedEdge(String url) {
      try {
        new ProcessBuilder("cmd", "/c", "start", "msedge", url).start();

        running = true;
        return true;
      } catch (Exception ignored) {
        return false;
      }
    }

    private boolean openedDefaultBrowser(String url) throws Exception {
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(new URI(url));
        return true;
      } else {
        return false;
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
  }
}
