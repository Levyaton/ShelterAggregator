package cz.levy.pet.shelter.aggregator.api;

import cz.levy.pet.shelter.aggregator.domain.Sex;
import java.util.List;

public record DogRequest(
    String externalId,
    String shelterUrl,
    String name,
    String description,
    String breedGuess,
    Sex sex,
    Float estimatedAge,
    Float currentWeight,
    Float estimatedFinalWeightMin,
    Float estimatedFinalWeightMax,
    String dogAddress,
    List<byte[]> images) {}
