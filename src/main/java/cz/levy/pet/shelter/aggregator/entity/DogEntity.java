package cz.levy.pet.shelter.aggregator.entity;

import cz.levy.pet.shelter.aggregator.domain.Sex;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
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
  @Column(name = "sex", nullable = false, columnDefinition = "sex_enum")
  private Sex sex;

  @Column private Float estimatedAgeInYears;

  @Column private Float currentWeight;

  @Column private Float estimatedFinalWeightMin;

  @Column private Float estimatedFinalWeightMax;

  @Column private String dogAddress;

  @ManyToOne(optional = false)
  @JoinColumn(name = "shelter_id", nullable = false)
  private ShelterEntity shelter;
}
