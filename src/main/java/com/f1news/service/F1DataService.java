package com.f1news.service;

import com.f1news.model.dto.DriverStandingDto;
import com.f1news.model.dto.RaceDto;
import com.f1news.model.dto.RaceResultDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class F1DataService {

    private static final Logger log = LoggerFactory.getLogger(F1DataService.class);

    private final WebClient webClient;

    public F1DataService(WebClient f1WebClient) {
        this.webClient = f1WebClient;
    }

    public List<RaceDto> getCurrentSeasonSchedule() {
        try {
            JsonNode root = webClient.get()
                    .uri("/current.json")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<RaceDto> races = new ArrayList<>();
            JsonNode raceArray = root.path("MRData").path("RaceTable").path("Races");
            LocalDate today = LocalDate.now();

            for (JsonNode race : raceArray) {
                String date = race.path("date").asText();
                LocalDate raceDate = LocalDate.parse(date);
                races.add(new RaceDto(
                        race.path("season").asText(),
                        race.path("round").asText(),
                        race.path("raceName").asText(),
                        race.path("Circuit").path("circuitName").asText(),
                        race.path("Circuit").path("Location").path("country").asText(),
                        race.path("Circuit").path("Location").path("locality").asText(),
                        date,
                        race.path("time").asText("TBC"),
                        raceDate.isBefore(today)
                ));
            }
            return races;
        } catch (Exception e) {
            log.error("Failed to fetch season schedule", e);
            return List.of();
        }
    }

    public Optional<RaceDto> getNextRace() {
        return getCurrentSeasonSchedule().stream()
                .filter(r -> !r.isCompleted())
                .findFirst();
    }

    public List<RaceResultDto> getRecentResults(int count) {
        try {
            JsonNode root = webClient.get()
                    .uri("/current/results.json?limit=100")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<RaceResultDto> results = new ArrayList<>();
            JsonNode raceArray = root.path("MRData").path("RaceTable").path("Races");

            for (JsonNode race : raceArray) {
                List<RaceResultDto.FinisherDto> finishers = new ArrayList<>();
                for (JsonNode r : race.path("Results")) {
                    String time = r.path("Time").path("time").asText("");
                    if (time.isBlank()) {
                        time = r.path("status").asText("DNF");
                    }
                    finishers.add(new RaceResultDto.FinisherDto(
                            r.path("position").asText(),
                            r.path("Driver").path("code").asText(),
                            r.path("Driver").path("givenName").asText() + " " + r.path("Driver").path("familyName").asText(),
                            r.path("Constructor").path("name").asText(),
                            r.path("points").asText(),
                            time,
                            r.path("status").asText()
                    ));
                }
                results.add(new RaceResultDto(
                        race.path("raceName").asText(),
                        race.path("Circuit").path("circuitName").asText(),
                        race.path("Circuit").path("Location").path("country").asText(),
                        race.path("date").asText(),
                        race.path("round").asText(),
                        finishers
                ));
            }

            // Return most recent races (API returns oldest first)
            int fromIndex = Math.max(0, results.size() - count);
            List<RaceResultDto> recent = new ArrayList<>(results.subList(fromIndex, results.size()));
            java.util.Collections.reverse(recent);
            return recent;
        } catch (Exception e) {
            log.error("Failed to fetch recent results", e);
            return List.of();
        }
    }

    public List<DriverStandingDto> getDriverStandings() {
        try {
            JsonNode root = webClient.get()
                    .uri("/current/driverStandings.json")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<DriverStandingDto> standings = new ArrayList<>();
            JsonNode lists = root.path("MRData").path("StandingsTable").path("StandingsLists");

            if (lists.isEmpty()) return List.of();

            for (JsonNode s : lists.get(0).path("DriverStandings")) {
                String team = s.path("Constructors").isEmpty()
                        ? "Unknown"
                        : s.path("Constructors").get(0).path("name").asText();

                standings.add(new DriverStandingDto(
                        s.path("position").asText(),
                        s.path("Driver").path("code").asText(),
                        s.path("Driver").path("givenName").asText() + " " + s.path("Driver").path("familyName").asText(),
                        team,
                        s.path("points").asText(),
                        s.path("wins").asText(),
                        s.path("Driver").path("nationality").asText()
                ));
            }
            return standings;
        } catch (Exception e) {
            log.error("Failed to fetch driver standings", e);
            return List.of();
        }
    }

    public List<DriverStandingDto> getConstructorStandings() {
        try {
            JsonNode root = webClient.get()
                    .uri("/current/constructorStandings.json")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<DriverStandingDto> standings = new ArrayList<>();
            JsonNode lists = root.path("MRData").path("StandingsTable").path("StandingsLists");

            if (lists.isEmpty()) return List.of();

            for (JsonNode s : lists.get(0).path("ConstructorStandings")) {
                standings.add(new DriverStandingDto(
                        s.path("position").asText(),
                        s.path("Constructor").path("constructorId").asText().toUpperCase(),
                        s.path("Constructor").path("name").asText(),
                        s.path("Constructor").path("nationality").asText(),
                        s.path("points").asText(),
                        s.path("wins").asText(),
                        ""
                ));
            }
            return standings;
        } catch (Exception e) {
            log.error("Failed to fetch constructor standings", e);
            return List.of();
        }
    }

    /**
     * Fetch results from a specific past race (by round) for recent form analysis.
     */
    public List<RaceResultDto.FinisherDto> getRoundResults(String round) {
        try {
            JsonNode root = webClient.get()
                    .uri("/current/" + round + "/results.json")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<RaceResultDto.FinisherDto> finishers = new ArrayList<>();
            JsonNode races = root.path("MRData").path("RaceTable").path("Races");
            if (races.isEmpty()) return List.of();

            for (JsonNode r : races.get(0).path("Results")) {
                finishers.add(new RaceResultDto.FinisherDto(
                        r.path("position").asText(),
                        r.path("Driver").path("code").asText(),
                        r.path("Driver").path("givenName").asText() + " " + r.path("Driver").path("familyName").asText(),
                        r.path("Constructor").path("name").asText(),
                        r.path("points").asText(),
                        r.path("Time").path("time").asText(""),
                        r.path("status").asText()
                ));
            }
            return finishers;
        } catch (Exception e) {
            log.error("Failed to fetch round {} results", round, e);
            return List.of();
        }
    }
}
