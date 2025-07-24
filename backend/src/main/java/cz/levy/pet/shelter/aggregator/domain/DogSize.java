package cz.levy.pet.shelter.aggregator.domain;

import lombok.Getter;

@Getter
public enum DogSize {
  SMALL(0),
  MEDIUM(12),
  LARGE(24);

  private final int from;

  DogSize(int from) {
    this.from = from;
  }
}
