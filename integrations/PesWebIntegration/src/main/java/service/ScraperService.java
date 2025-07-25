package service;

import cz.levy.pet.shelter.aggregator.domain.DogSize;
import cz.levy.pet.shelter.aggregator.domain.Sex;
import cz.levy.pet.shelter.aggregator.dto.DogDto;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScraperService {

  private static final String BASE = "https://www.pesweb.cz";
  private static final int PAGE_SIZE = 52;
  private static final String SEARCH_DOGS = "/cz/psi-k-adopci";
  private static final String LEGAL_DISCLAIMER =
      "Žádná část těchto stránek, včetně fotografií, nesmí být kopírována a rozmnožována za účelem rozšiřování v jakékoliv formě či jakýmkoliv způsobem bez písemného souhlasu provozovatele portálu.";

  @Autowired private TaskExecutor scraperExecutor;

  @Scheduled(fixedDelayString = "${scrape.interval:3600000}")
  public void dispatchPages() throws Exception {
    Document doc =
        Jsoup.connect(BASE + SEARCH_DOGS).userAgent("MyScraper/1.0").timeout(10_000).get();

    Element pager = doc.selectFirst("div.pager");
    int lastPage = Integer.parseInt(pager.select("a[rel=next]").last().text());

    for (int page = 0; page < lastPage; page++) {
      final int offset = page * PAGE_SIZE;
      scraperExecutor.execute(() -> scrapeDogListPage(offset));
    }
  }

  private void scrapeDogListPage(int offset) {
    try {
      Document pageDoc =
          Jsoup.connect(BASE + SEARCH_DOGS + "?start=" + offset)
              .userAgent("MyScraper/1.0")
              .timeout(10_000)
              .get();

      pageDoc.select("div.object-item ").stream()
          .map(el -> el.attr("href"))
          .forEach(this::submitDogDetailJob);

    } catch (IOException e) {
    }
  }

  private void submitDogDetailJob(String dogUrl) {
    scraperExecutor.execute(() -> scrapeDogDetail(dogUrl));
  }

  private static DogDto scrapeDogDetail(String url) {
    try {
      var fullUrl = BASE + url;
      Document detail = Jsoup.connect(fullUrl).userAgent("MyScraper/1.0").timeout(10_000).get();

      var externalId = url.replace("/cz/psi-k-adopci?objid=", "");
      Elements imageLinkElements = detail.select("div.gallery-basic a[data-fancybox]");
      var imageLinks = imageLinkElements.stream().map(link -> link.absUrl("href")).toList();

      var dogName = detail.selectFirst(".nadpis").text();
      var breedGuess = getBreed(detail);
      var sex = getSex(detail);
      var age = getAge(detail);
      var dogSize = getWeight(detail);
      var dogAddress = getAddress(detail);
      var dogDescription = getDescription(detail);

      return DogDto.builder()
          .shelterId(1L)
          .externalId(externalId)
          .imageUrls(imageLinks)
          .estimatedAgeInYears(age)
          .estimatedFinalWeightMin(dogSize.getFirst().floatValue())
          .estimatedFinalWeightMax(dogSize.getSecond().floatValue())
          .shelterUrl(fullUrl)
          .name(dogName)
          .breedGuess(breedGuess)
          .sex(sex)
          .dogAddress(dogAddress)
          .description(dogDescription)
          .build();

    } catch (IOException e) {
      return null;
    }
  }

  static String getDescription(Element detail) {
    var customSuffix = " Více informací o pejskovi naleznete na profilu pejska na pesweb.cz.";
    return detail.select("div p").text().replace(LEGAL_DISCLAIMER, "") + customSuffix;
  }

  static Pair<Integer, Integer> getWeight(Element detail) {
    String weightText =
        detail
            .selectFirst("#dogdetail #dd-left table tbody tr td:contains(Velikost)")
            .text()
            .split(" ")[1]
            .strip();
    switch (weightText) {
      case "Malý" -> {
        return Pair.of(DogSize.SMALL.getFrom(), DogSize.MEDIUM.getFrom());
      }
      case "Střední" -> {
        return Pair.of(DogSize.MEDIUM.getFrom(), DogSize.LARGE.getFrom());
      }
      case "Velký" -> {
        return Pair.of(DogSize.LARGE.getFrom(), 55);
      }
      default -> {
        return Pair.of(0, 55);
      }
    }
  }

  static String getAddress(Element detail) {
    String address =
        detail
            .selectFirst("tr:contains(umístění) td")
            .childNodes()
            .getFirst()
            .attributes()
            .attribute("href")
            .getValue();
    return BASE + address;
  }

  static Float getAge(Element detail) {
    Element ageElement = detail.selectFirst("td:contains(věk)");
    if (ageElement == null) {
      return null;
    }
    var age = ageElement.text();
    var ageStringParts = age.split(" ");
    if (ageStringParts[2].equals("měsíce")) {
      return Float.parseFloat(ageStringParts[1]) / 12;
    } else {
      return Float.parseFloat(ageStringParts[1]);
    }
  }

  static Sex getSex(Element detail) {
    Element sexElement = detail.selectFirst("td:contains(Pohlaví)");
    if (sexElement == null) {
      return null;
    }
    var sexText = sexElement.text().split(" ")[1].strip();
    return switch (sexText) {
      case "Fena" -> Sex.FEMALE;
      case "Pes" -> Sex.MALE;
      default -> null;
    };
  }

  static String getBreed(Element detail) {
    var breed = detail.selectFirst("#breed");
    if (breed == null) {
      return null;
    }
    var mainBreed = breed.textNodes().getLast().text().strip();
    var breedGuesses = breed.select("a").stream().map(Element::text).toList();
    return mainBreed + ", " + String.join(",", breedGuesses).strip();
  }
}
