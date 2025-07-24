package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.levy.pet.shelter.aggregator.domain.Sex;
import cz.levy.pet.shelter.aggregator.dto.DogDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import static java.io.IO.println;
import static service.ScraperService.*;

public class DogExporter {

    private static final String BASE        = "https://www.pesweb.cz";
    private static final String SEARCH_PATH = "/cz/psi-k-adopci";
    private static final int    PAGE_SIZE   = 52;

    public static void exportAllDogs(String outputJsonPath) throws Exception {
        // 1) Collect all detail paths
        List<String> detailPaths = new ArrayList<>();
        Document first = Jsoup.connect(BASE + SEARCH_PATH)
                .userAgent("MyScraper/1.0")
                .timeout(10_000)
                .get();

        int lastPage = Integer.parseInt(
                first.selectFirst("div.pager a[rel=next]").text()
        );

        for (int page = 0; page < lastPage; page++) {
            int offset = page * PAGE_SIZE;
            Document pageDoc = Jsoup.connect(BASE + SEARCH_PATH + "?start=" + offset)
                    .userAgent("MyScraper/1.0")
                    .timeout(10_000)
                    .get();

            for (Element card : pageDoc.select("div.object-item a")) {
                detailPaths.add(card.attr("href"));
            }
        }

        var deduplicatedPaths = new LinkedHashSet<>(detailPaths);

        // 2) Scrape details in parallel (4 threads)
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<DogDto>> futures = new ArrayList<>(detailPaths.size());
        for (String path : deduplicatedPaths) {
            futures.add(exec.submit(() -> scrapeDogDetail(path)));
        }
        exec.shutdown();
        // wait up to 30 minutes for all tasks (adjust as needed)
        if (!exec.awaitTermination(30, TimeUnit.MINUTES)) {
            throw new RuntimeException("Timed out waiting for scraper threads");
        }

        List<DogDto> allDogs = new ArrayList<>(futures.stream().map(
                future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Error processing dog detail: " + e.getMessage());
                        return null;
                    }
                }
        )
                .filter(Objects::nonNull)
                .toList());

        // 4) Serialize to JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(outputJsonPath), allDogs);

        System.out.printf("Exported %d unique dogs to %s%n",
                allDogs.size(), outputJsonPath);
    }

    // Your existing detail scrape logic (copy these helpers in)
    private static DogDto scrapeDogDetail(String detailPath) {
        try {
            String fullUrl = BASE + detailPath;
            Document detail = Jsoup.connect(fullUrl)
                    .userAgent("MyScraper/1.0")
                    .timeout(10_000)
                    .get();

            String externalId = detailPath.replace("/cz/psi-k-adopci?objid=", "");
            var imageLinks = detail.select("div.gallery-basic a[data-fancybox]")
                    .stream()
                    .map(el -> el.absUrl("href"))
                    .toList();

            String name      = detail.selectFirst(".nadpis").text();
            String breed     = getBreed(detail);
            Sex sex       = getSex(detail);
            Float  age       = getAge(detail);
            Pair<Integer,Integer> weight = getWeight(detail);
            String address   = getAddress(detail);
            String desc      = getDescription(detail);

            println("Scraped dog: " + name + " (" + externalId + ")");
            return DogDto.builder()
                    .shelterId(1L)
                    .externalId(externalId)
                    .imageUrls(imageLinks)
                    .estimatedAgeInYears(age)
                    .estimatedFinalWeightMin(weight.getFirst().floatValue())
                    .estimatedFinalWeightMax(weight.getSecond().floatValue())
                    .shelterUrl(fullUrl)
                    .name(name)
                    .breedGuess(breed)
                    .sex(sex)
                    .dogAddress(address)
                    .description(desc)
                    .build();

        } catch (IOException e) {
            System.err.println("Error scraping " + detailPath + ": " + e.getMessage());
            return null;
        }
    }
}
