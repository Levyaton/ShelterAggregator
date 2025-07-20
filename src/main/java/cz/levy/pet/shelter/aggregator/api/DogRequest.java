package cz.levy.pet.shelter.aggregator.api;

import cz.levy.pet.shelter.aggregator.domain.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record DogRequest(
    @NotBlank String externalId,
    String shelterUrl,
    @NotBlank String name,
    String description,
    String breedGuess,
    @NotNull Sex sex,
    @PositiveOrZero Float estimatedAgeInYears,
    @Positive Float currentWeight,
    @Positive Float estimatedFinalWeightMin,
    @Positive Float estimatedFinalWeightMax,
    String dogAddress,
    List<byte[]> images) {}
