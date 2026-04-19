package com.f1news.controller;

import com.f1news.service.F1DataService;
import com.f1news.service.PredictionService;
import com.f1news.service.WebPushService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final F1DataService f1DataService;
    private final PredictionService predictionService;
    private final WebPushService webPushService;

    public HomeController(F1DataService f1DataService, PredictionService predictionService,
                          WebPushService webPushService) {
        this.f1DataService = f1DataService;
        this.predictionService = predictionService;
        this.webPushService = webPushService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("nextRace", f1DataService.getNextRace().orElse(null));
        model.addAttribute("recentResults", f1DataService.getRecentResults(5));
        model.addAttribute("driverStandings", f1DataService.getDriverStandings().stream().limit(10).toList());
        model.addAttribute("prediction", predictionService.predictNextRace().orElse(null));
        model.addAttribute("schedule", f1DataService.getCurrentSeasonSchedule());
        model.addAttribute("vapidPublicKey", webPushService.getPublicVapidKey());
        model.addAttribute("subscriberCount", webPushService.getSubscriberCount());
        return "index";
    }
}
