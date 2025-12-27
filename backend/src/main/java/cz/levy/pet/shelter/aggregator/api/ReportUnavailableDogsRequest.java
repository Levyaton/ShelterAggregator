package cz.levy.pet.shelter.aggregator.api;

import java.util.List;

public record ReportUnavailableDogsRequest(List<Long> dogIds) {}

