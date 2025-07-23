package cz.levy.pet.shelter.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@Profile("local-simulated")
@SpringBootApplication
public class LocalSimulatedApplication {
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(LocalSimulatedApplication.class);
    app.setAdditionalProfiles("local-simulated");
    app.run(args);
  }
}
