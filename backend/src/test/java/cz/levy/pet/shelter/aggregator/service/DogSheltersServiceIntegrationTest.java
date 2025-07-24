package cz.levy.pet.shelter.aggregator.service;

import static cz.levy.pet.shelter.aggregator.utils.TestUtils.assertThatEqualsRecursive;

import cz.levy.pet.shelter.aggregator.config.TestContainerConfig;
import cz.levy.pet.shelter.aggregator.domain.DogSize;
import cz.levy.pet.shelter.aggregator.domain.Sex;
import cz.levy.pet.shelter.aggregator.domain.SortField;
import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import cz.levy.pet.shelter.aggregator.entity.ShelterEntity;
import cz.levy.pet.shelter.aggregator.repository.DogRepository;
import cz.levy.pet.shelter.aggregator.repository.ShelterRepository;
import java.util.List;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
public class DogSheltersServiceIntegrationTest {
  @Autowired private DogSheltersService dogSheltersService;
  @Autowired private DogRepository dogRepository;
  @Autowired private ShelterRepository shelterRepository;

  @BeforeAll
  void seedDatabase() {
    dogRepository.deleteAll();
    shelterRepository.deleteAll();

    var testShelter =
        shelterRepository.save(
            ShelterEntity.builder().name("Seed Shelter").address("Seed Address").build());

    var dog1SmallF =
        dogRepository.save(
            DogEntity.builder()
                .externalId("dog1")
                .name("d1")
                .sex(Sex.FEMALE)
                .estimatedAgeInYears(2F)
                .currentWeight(8F)
                .estimatedFinalWeightMin(8F)
                .estimatedFinalWeightMax(8F)
                .shelter(testShelter)
                .build());
    var dog2MediumMidF =
        dogRepository.save(
            DogEntity.builder()
                .externalId("dog2")
                .name("d2")
                .sex(Sex.MALE)
                .estimatedAgeInYears(5F)
                .currentWeight(15F)
                .estimatedFinalWeightMin(15F)
                .estimatedFinalWeightMax(15F)
                .shelter(testShelter)
                .build());

    var dog3LargeOldM =
        dogRepository.save(
            DogEntity.builder()
                .externalId("dog3")
                .name("d3")
                .sex(Sex.MALE)
                .estimatedAgeInYears(8F)
                .currentWeight(30F)
                .estimatedFinalWeightMin(30F)
                .estimatedFinalWeightMax(30F)
                .shelter(testShelter)
                .build());
    var dog4SmallSeniorF =
        dogRepository.save(
            DogEntity.builder()
                .externalId("dog4")
                .name("d4")
                .sex(Sex.FEMALE)
                .estimatedAgeInYears(12F)
                .currentWeight(5F)
                .estimatedFinalWeightMin(5F)
                .estimatedFinalWeightMax(5F)
                .shelter(testShelter)
                .build());
    var dog5MediumMidF =
        dogRepository.save(
            DogEntity.builder()
                .externalId("dog5")
                .name("d5")
                .sex(Sex.FEMALE)
                .estimatedAgeInYears(7F)
                .currentWeight(20F)
                .estimatedFinalWeightMin(20F)
                .estimatedFinalWeightMax(20F)
                .shelter(testShelter)
                .build());

    var dog6SmallNoAgeM =
        dogRepository.save(
            DogEntity.builder()
                .externalId("dog6")
                .name("d6")
                .sex(Sex.MALE)
                .estimatedAgeInYears(null)
                .currentWeight(10F)
                .estimatedFinalWeightMin(10F)
                .estimatedFinalWeightMax(10F)
                .shelter(testShelter)
                .build());
    var dog7NoDataF =
        dogRepository.save(
            DogEntity.builder()
                .externalId("dog7")
                .name("d7")
                .sex(Sex.FEMALE)
                .estimatedAgeInYears(null)
                .currentWeight(null)
                .estimatedFinalWeightMin(null)
                .estimatedFinalWeightMax(null)
                .shelter(testShelter)
                .build());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("filterPaginationCases")
  void paginateAndFilterDogsReturnsExpectedResults(FilterPaginationTestCase caseData) {
    var pageable =
        PageRequest.of(
            caseData.page, caseData.size, Sort.by(caseData.order, caseData.sort.getFieldName()));

    Page<DogEntity> page =
        dogSheltersService.paginateAndFilterDogs(
            pageable, caseData.ageMin, caseData.ageMax, caseData.sex, caseData.dogSize);

    assertThatEqualsRecursive(
        page.map(DogEntity::getExternalId).stream().toList(), caseData.expectedExternalIds);
  }

  static Stream<FilterPaginationTestCase> filterPaginationCases() {
    return Stream.of(
        FilterPaginationTestCase.builder()
            .caseName("NO FILTER, Pagination test")
            .page(1)
            .size(4)
            .order(Sort.Direction.ASC)
            .sort(SortField.ID)
            .expectedExternalIds(List.of("dog5", "dog6", "dog7"))
            .build(),
        FilterPaginationTestCase.builder()
            .caseName("FILTER ageMin ≥ 6")
            .page(0)
            .size(10)
            .order(Sort.Direction.ASC)
            .sort(SortField.ID)
            .ageMin(6F)
            .expectedExternalIds(List.of("dog3", "dog4", "dog5"))
            .build(),
        FilterPaginationTestCase.builder()
            .caseName("FILTER ageMax ≤ 5")
            .page(0)
            .size(10)
            .order(Sort.Direction.ASC)
            .sort(SortField.ID)
            .ageMax(5F)
            .expectedExternalIds(List.of("dog1", "dog2"))
            .build(),
        FilterPaginationTestCase.builder()
            .caseName("FILTER sex = FEMALE, FILTER ageMax ≤ 8")
            .page(0)
            .size(10)
            .order(Sort.Direction.ASC)
            .sort(SortField.ID)
            .sex(Sex.FEMALE)
            .ageMax(8F)
            .expectedExternalIds(List.of("dog1", "dog5"))
            .build(),
        FilterPaginationTestCase.builder()
            .caseName("FILTER size = SMALL, sort by WEIGHT desc")
            .page(0)
            .size(10)
            .order(Sort.Direction.DESC)
            .sort(SortField.CURRENT_WEIGHT)
            .dogSize(DogSize.SMALL)
            .expectedExternalIds(List.of("dog6", "dog1", "dog4"))
            .build());
  }

  @Data
  @Builder
  public static class FilterPaginationTestCase {
    private String caseName;
    private int page;
    private int size;
    private Sort.Direction order;
    private SortField sort;
    @Builder.Default private Float ageMin = null;
    @Builder.Default private Float ageMax = null;
    @Builder.Default private Sex sex = null;
    @Builder.Default private DogSize dogSize = null;

    private List<String> expectedExternalIds;

    @Override
    public String toString() {
      return caseName;
    }
  }
}
