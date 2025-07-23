package cz.levy.pet.shelter.aggregator.spec;

import static cz.levy.pet.shelter.aggregator.domain.DogSize.LARGE;
import static cz.levy.pet.shelter.aggregator.domain.DogSize.MEDIUM;

import cz.levy.pet.shelter.aggregator.domain.DogSize;
import cz.levy.pet.shelter.aggregator.domain.Sex;
import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import org.springframework.data.jpa.domain.Specification;

public class DogSpec {

  public static Specification<DogEntity> ageGte(Float minAge) {
    return (root, query, cb) ->
        cb.and(
            cb.isNotNull(root.get("estimatedAgeInYears")),
            cb.ge(root.get("estimatedAgeInYears"), minAge));
  }

  public static Specification<DogEntity> ageLte(Float maxAge) {
    return (root, query, cb) ->
        cb.and(
            cb.isNotNull(root.get("estimatedAgeInYears")),
            cb.le(root.get("estimatedAgeInYears"), maxAge));
  }

  public static Specification<DogEntity> hasSex(Sex sex) {
    return (root, query, cb) -> cb.equal(root.get("sex"), sex);
  }

  public static Specification<DogEntity> hasSize(DogSize size) {
    return switch (size) {
      case SMALL ->
          (root, query, cb) ->
              cb.and(
                  cb.isNotNull(root.get("estimatedFinalWeightMax")),
                  cb.le(root.get("estimatedFinalWeightMax"), MEDIUM.getFrom()));
      case MEDIUM ->
          (root, query, cb) ->
              cb.and(
                  cb.isNotNull(root.get("estimatedFinalWeightMin")),
                  cb.isNotNull(root.get("estimatedFinalWeightMax")),
                  cb.gt(root.get("estimatedFinalWeightMax"), MEDIUM.getFrom()),
                  cb.le(root.get("estimatedFinalWeightMax"), LARGE.getFrom()));
      case LARGE ->
          (root, query, cb) ->
              cb.and(
                  cb.isNotNull(root.get("estimatedFinalWeightMin")),
                  cb.gt(root.get("estimatedFinalWeightMin"), LARGE.getFrom()));
    };
  }
}
