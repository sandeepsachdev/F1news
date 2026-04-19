package com.f1news.controller;

import com.f1news.service.F1DataService;
import com.f1news.service.PredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/f1")
public class ApiController {

    private final F1DataService f1DataService;
    private final PredictionService predictionService;

    public ApiController(F1DataService f1DataService, PredictionService predictionService) {
        this.f1DataService = f1DataService;
        this.predictionService = predictionService;
    }

    @GetMapping("/schedule")
    public ResponseEntity<?> schedule() {
        return ResponseEntity.ok(f1DataService.getCurrentSeasonSchedule());
    }

    @GetMapping("/next-race")
    public ResponseEntity<?> nextRace() {
        return f1DataService.getNextRace()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/results")
    public ResponseEntity<?> results(@RequestParam(defaultValue = "5") int count) {
        return ResponseEntity.ok(f1DataService.getRecentResults(Math.min(count, 10)));
    }

    @GetMapping("/standings/drivers")
    public ResponseEntity<?> driverStandings() {
        return ResponseEntity.ok(f1DataService.getDriverStandings());
    }

    @GetMapping("/standings/constructors")
    public ResponseEntity<?> constructorStandings() {
        return ResponseEntity.ok(f1DataService.getConstructorStandings());
    }

    @GetMapping("/predictions")
    public ResponseEntity<?> predictions() {
        return predictionService.predictNextRace()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
