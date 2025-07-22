package cz.levy.pet.shelter.aggregator.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Entity
@Data
public class ShelterEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private String name;

  @Column private String address;
  @Column private String phoneNumber;
  @Column private String url;
  @Column private String email;
  @Column private boolean isNonProfit;
  @Column private String bankAccountNumber;
  @OneToMany(mappedBy = "shelter", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<DogEntity> dogs = new HashSet<>();
}
