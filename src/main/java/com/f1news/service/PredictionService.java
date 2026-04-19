package com.f1news.service;

import com.f1news.model.dto.DriverStandingDto;
import com.f1news.model.dto.RaceDto;
import com.f1news.model.dto.RacePredictionDto;
import com.f1news.model.dto.RaceResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates race predictions using weighted championship standings and recent form.
 * Scoring: 60% season points rank + 40% average finishing position in last 3 races.
 */
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    private final F1DataService f1DataService;

    public PredictionService(F1DataService f1DataService) {
        this.f1DataService = f1DataService;
    }

    public Optional<RacePredictionDto> predictNextRace() {
        Optional<RaceDto> nextRace = f1DataService.getNextRace();
        if (nextRace.isEmpty()) {
            return Optional.empty();
        }

        RaceDto race = nextRace.get();
        List<DriverStandingDto> standings = f1DataService.getDriverStandings();
        if (standings.isEmpty()) {
            return Optional.empty();
        }

        // Build championship score map (normalized 0-100)
        double maxPoints = standings.stream()
                .mapToDouble(s -> Double.parseDouble(s.points().isEmpty() ? "0" : s.points()))
                .max()
                .orElse(1.0);

        Map<String, Double> champScore = new LinkedHashMap<>();
        for (DriverStandingDto s : standings) {
            double pts = Double.parseDouble(s.points().isEmpty() ? "0" : s.points());
            champScore.put(s.driverCode(), (pts / maxPoints) * 100.0);
        }

        // Build recent form score from last 3 completed races
        List<RaceResultDto> recentRaces = f1DataService.getRecentResults(3);
        Map<String, List<Integer>> recentPositions = new HashMap<>();

        for (RaceResultDto raceResult : recentRaces) {
            for (RaceResultDto.FinisherDto f : raceResult.results()) {
                try {
                    int pos = Integer.parseInt(f.position());
                    recentPositions.computeIfAbsent(f.driverCode(), k -> new ArrayList<>()).add(pos);
                } catch (NumberFormatException ignored) {
                    // DNF/DSQ — treat as position 20
                    recentPositions.computeIfAbsent(f.driverCode(), k -> new ArrayList<>()).add(20);
                }
            }
        }

        // Recent form score: invert average position (lower is better), normalize to 0-100
        Map<String, Double> formScore = new HashMap<>();
        for (var entry : recentPositions.entrySet()) {
            double avgPos = entry.getValue().stream().mapToInt(i -> i).average().orElse(10.0);
            formScore.put(entry.getKey(), Math.max(0, 100.0 - ((avgPos - 1) / 19.0 * 100.0)));
        }

        // Combined score: 60% championship, 40% recent form
        List<RacePredictionDto.PredictedFinisher> predictions = new ArrayList<>();
        Map<String, String> driverNames = standings.stream()
                .collect(Collectors.toMap(DriverStandingDto::driverCode, DriverStandingDto::driverName));
        Map<String, String> driverTeams = standings.stream()
                .collect(Collectors.toMap(DriverStandingDto::driverCode, DriverStandingDto::team));

        for (DriverStandingDto s : standings) {
            String code = s.driverCode();
            double champ = champScore.getOrDefault(code, 0.0);
            double form = formScore.getOrDefault(code, 50.0);
            double combined = (champ * 0.6) + (form * 0.4);

            String reasoning = buildReasoning(s, form, recentPositions.get(code));
            predictions.add(new RacePredictionDto.PredictedFinisher(
                    0, // position assigned after sort
                    code,
                    driverNames.getOrDefault(code, s.driverName()),
                    driverTeams.getOrDefault(code, s.team()),
                    Math.round(combined * 10.0) / 10.0,
                    reasoning
            ));
        }

        predictions.sort(Comparator.comparingDouble(RacePredictionDto.PredictedFinisher::score).reversed());

        // Assign predicted positions
        List<RacePredictionDto.PredictedFinisher> ranked = new ArrayList<>();
        for (int i = 0; i < Math.min(predictions.size(), 10); i++) {
            var p = predictions.get(i);
            ranked.add(new RacePredictionDto.PredictedFinisher(
                    i + 1, p.driverCode(), p.driverName(), p.team(), p.score(), p.reasoning()
            ));
        }

        return Optional.of(new RacePredictionDto(
                race.raceName(),
                race.circuitName(),
                race.country(),
                race.date(),
                ranked,
                "Prediction based on 60% championship standings + 40% recent form (last 3 races)"
        ));
    }

    private String buildReasoning(DriverStandingDto standing, double formScore, List<Integer> recentPos) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(standing.position()).append(" in championship (").append(standing.points()).append(" pts)");
        if (recentPos != null && !recentPos.isEmpty()) {
            String posStr = recentPos.stream().map(p -> "P" + p).collect(Collectors.joining(", "));
            sb.append("; recent finishes: ").append(posStr);
        } else {
            sb.append("; no recent race data");
        }
        return sb.toString();
    }
}
