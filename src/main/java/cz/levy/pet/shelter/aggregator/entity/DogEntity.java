package cz.levy.pet.shelter.aggregator.entity;

import cz.levy.pet.shelter.aggregator.domain.Sex;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private String externalId;

  @Column private String shelterUrl;

  @Column(nullable = false)
  private String name;

  @Column private String description;

  @Column private String breedGuess;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Sex sex;

  @Column private Float estimatedAgeInYears;

  @Column private Float currentWeight;

  @Column private Float estimatedFinalWeightMin;

  @Column private Float estimatedFinalWeightMax;

  @Column private String dogAddress;

  @ManyToOne(optional = false)
  @JoinColumn(name = "shelter_id", nullable = false)
  private ShelterEntity shelter;

  @ElementCollection
  @CollectionTable(name = "dog_image_urls", joinColumns = @JoinColumn(name = "dog_id"))
  @Column(name = "image_url", length = 2048)
  private List<String> imageUrls = new ArrayList<>();
}
